package org.oddlama.vane.busybar.common

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpsConfigurator
import com.sun.net.httpserver.HttpsServer
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLContext

/**
 * HTTP server that BUSY Bar bridges connect to, serving HTTPS when given a TLS context.
 *
 * - `GET /events` keeps a server-sent-events stream open. The first event is always `snapshot`.
 * - `POST /action` accepts a JSON request such as `{"type":"presence.set","busy":true}`.
 * - `GET /icon` returns the server icon as PNG, with an ETag the snapshot also carries. Behind a
 *   proxy, each bridge gets the icon of the server its owner is on, see [Host.iconKey].
 * - `GET /head/<id>` returns a player's 8x8 face as PNG, for the skin id in join and quit events.
 *
 * Every endpoint requires `Authorization: Bearer <token>`; [Host] decides whose token it is, what
 * the snapshot holds and what requests do. Every connection runs on its own virtual thread; events
 * are handed over through a bounded queue per connection, so callers never block on a slow bridge.
 *
 * @param host the plugin serving the bridges.
 * @param heads player faces served by `/head`.
 * @param log where problems are reported.
 */
class BridgeServer(private val host: Host, private val heads: Heads, private val log: BridgeLog) {
    /** What the plugin serving the bridges decides. Called from HTTP threads. */
    interface Host {
        /** The player owning [token], or null when it is unknown. */
        fun playerForToken(token: String): UUID?

        /** The first event for a connecting bridge of [player]. May block briefly; null answers 503. */
        fun snapshot(player: UUID): JSONObject?

        /**
         * Handles a parsed `POST /action` body from [player]'s bridge and returns the HTTP status:
         * 202 when queued, 400 for unknown requests, 403 when not allowed.
         */
        fun request(player: UUID, request: JSONObject): Int

        /** [player] now has [count] bridges connected. */
        fun onBridgesChanged(player: UUID, count: Int) {}

        /** Which icon set with [setIcon] [player]'s bridges get; null or an unknown key gets the default. */
        fun iconKey(player: UUID): String? = null
    }

    /** One connected bridge. */
    private class Client(val player: UUID) {
        /** Encoded frames waiting to be written, ended by [CLOSE]. One extra slot keeps room for it. */
        val queue = LinkedBlockingQueue<String>(QUEUE_CAPACITY + 1)
    }

    /** The server icon as served by `/icon`. */
    private class Icon(val png: ByteArray, val etag: String)

    private var httpServer: HttpServer? = null
    private var executor: ExecutorService? = null

    /** Connected bridges. */
    private val clients = CopyOnWriteArrayList<Client>()

    /** Monotonic SSE event id. */
    private val nextId = AtomicLong()

    /** Icons served by `/icon`, by key; [DEFAULT_ICON] is the default. */
    private val icons = ConcurrentHashMap<String, Icon>()

    /** ETag of the default server icon, or null when there is none. Bridges compare it to their cached copy. */
    fun iconTag(): String? = icons[DEFAULT_ICON]?.etag

    /** ETag of the icon [player]'s bridges get, or null when there is none. */
    fun iconTag(player: UUID): String? = iconFor(player)?.etag

    /** Sets the default icon served by `/icon` from a PNG, or removes it with null or empty data. */
    fun setIcon(png: ByteArray?) = setIcon(DEFAULT_ICON, png)

    /** Sets the icon under [key] from a PNG, or removes it with null or empty data. */
    fun setIcon(key: String, png: ByteArray?) {
        if (png == null || png.isEmpty()) {
            icons.remove(key)
            return
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(png).take(ETAG_BYTES).joinToString("") { "%02x".format(it) }
        icons[key] = Icon(png, "\"$hash\"")
    }

    private fun iconFor(player: UUID): Icon? = host.iconKey(player)?.let(icons::get) ?: icons[DEFAULT_ICON]

    /**
     * Binds the server to [address], with TLS when [ssl] is given.
     *
     * @throws IOException when the address cannot be bound.
     */
    fun start(address: InetSocketAddress, ssl: SSLContext?) {
        val exec = Executors.newVirtualThreadPerTaskExecutor()
        try {
            val server = if (ssl == null) {
                HttpServer.create(address, 0)
            } else {
                HttpsServer.create(address, 0).apply { httpsConfigurator = HttpsConfigurator(ssl) }
            }
            httpServer = server.apply {
                createContext("/events", ::handleEvents)
                createContext("/action", ::handleAction)
                createContext("/icon", ::handleIcon)
                createContext("/head/", ::handleHead)
                executor = exec
                start()
            }
            executor = exec
        } catch (e: IOException) {
            exec.shutdownNow()
            throw e
        }
    }

    /** Closes every bridge connection, after what is queued for it, and stops the server. */
    fun stop() {
        clients.forEach(::close)
        httpServer?.stop(1)
        httpServer = null
        executor?.shutdownNow()
        executor = null
    }

    /**
     * Queues an event for every connected bridge whose owner [receives] it. Safe to call from any
     * thread; [receives] is called once per bridge.
     */
    fun emit(type: String, data: JSONObject, receives: (UUID) -> Boolean) {
        if (clients.isEmpty()) return
        val frame = frame(type, data)
        for (client in clients) {
            if (!receives(client.player)) continue
            if (client.queue.size >= QUEUE_CAPACITY || !client.queue.offer(frame)) {
                log.warning("Dropping BUSY Bar bridge of ${client.player}: it is not keeping up with events.")
                close(client, discardPending = true)
            }
        }
    }

    /** Closes every bridge connection of [player]. */
    fun disconnect(player: UUID) = clients.filter { it.player == player }.forEach(::close)

    /** Number of bridges currently connected for [player]. */
    fun connectedCount(player: UUID): Int = clients.count { it.player == player }

    /** Players with at least one bridge connected. */
    fun connectedPlayers(): Set<UUID> = clients.mapTo(mutableSetOf()) { it.player }

    /**
     * Asks the handler of [client] to finish once it has written what is queued. A bridge that
     * fell behind ([discardPending]) loses its backlog instead, so the close marker fits.
     */
    private fun close(client: Client, discardPending: Boolean = false) {
        if (discardPending) client.queue.clear()
        if (!client.queue.offer(CLOSE)) {
            client.queue.clear()
            client.queue.offer(CLOSE)
        }
    }

    /** Encodes one server-sent event. */
    private fun frame(type: String, data: JSONObject): String =
        "id: ${nextId.incrementAndGet()}\nevent: $type\ndata: $data\n\n"

    /** Returns the player for the request's bearer token or null. */
    private fun authenticate(exchange: HttpExchange): UUID? {
        val header = exchange.requestHeaders.getFirst("Authorization") ?: return null
        if (!header.startsWith("Bearer ")) return null
        return host.playerForToken(header.removePrefix("Bearer ").trim())
    }

    /** Serves `GET /events`. */
    private fun handleEvents(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "GET") return exchange.sendResponseHeaders(405, -1)
            val player = authenticate(exchange) ?: return exchange.sendResponseHeaders(401, -1)
            val snapshot = host.snapshot(player) ?: return exchange.sendResponseHeaders(503, -1)

            exchange.responseHeaders.apply {
                add("Content-Type", "text/event-stream; charset=utf-8")
                add("Cache-Control", "no-cache")
            }
            exchange.sendResponseHeaders(200, 0)

            val client = Client(player)
            clients += client
            host.onBridgesChanged(player, connectedCount(player))
            try {
                val out = exchange.responseBody
                out.write(frame("snapshot", snapshot).toByteArray())
                out.flush()
                // Runs until the close marker, so frames queued before it (such as server.stop) are sent.
                while (true) {
                    val next = client.queue.poll(HEARTBEAT_SECONDS, TimeUnit.SECONDS)
                    if (next === CLOSE) break
                    out.write((next ?: HEARTBEAT).toByteArray())
                    out.flush()
                }
            } finally {
                clients -= client
                host.onBridgesChanged(player, connectedCount(player))
            }
        } catch (_: IOException) {
            // Bridge went away.
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            exchange.close()
        }
    }

    /** Serves `POST /action`. */
    private fun handleAction(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "POST") return exchange.sendResponseHeaders(405, -1)
            val player = authenticate(exchange) ?: return exchange.sendResponseHeaders(401, -1)

            val body = exchange.requestBody.readNBytes(MAX_REQUEST_BYTES + 1)
            if (body.size > MAX_REQUEST_BYTES) return exchange.sendResponseHeaders(413, -1)
            val request = try {
                JSONObject(String(body))
            } catch (_: JSONException) {
                return exchange.sendResponseHeaders(400, -1)
            }
            exchange.sendResponseHeaders(host.request(player, request), -1)
        } catch (_: IOException) {
            // Bridge went away.
        } finally {
            exchange.close()
        }
    }

    /** Serves `GET /icon`. */
    private fun handleIcon(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "GET") return exchange.sendResponseHeaders(405, -1)
            val player = authenticate(exchange) ?: return exchange.sendResponseHeaders(401, -1)
            val current = iconFor(player) ?: return exchange.sendResponseHeaders(404, -1)

            exchange.responseHeaders.add("ETag", current.etag)
            if (exchange.requestHeaders.getFirst("If-None-Match") == current.etag) {
                return exchange.sendResponseHeaders(304, -1)
            }
            exchange.responseHeaders.add("Content-Type", "image/png")
            exchange.sendResponseHeaders(200, current.png.size.toLong())
            exchange.responseBody.write(current.png)
        } catch (_: IOException) {
            // Bridge went away.
        } finally {
            exchange.close()
        }
    }

    /** Serves `GET /head/<id>`. Downloads the skin on first use, on this handler's own thread. */
    private fun handleHead(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "GET") return exchange.sendResponseHeaders(405, -1)
            if (authenticate(exchange) == null) return exchange.sendResponseHeaders(401, -1)
            val id = exchange.requestURI.path.removePrefix("/head/")
            val png = try {
                heads.face(id)
            } catch (e: IOException) {
                log.warning("Could not render the face for skin $id: ${e.message}")
                return exchange.sendResponseHeaders(502, -1)
            } ?: return exchange.sendResponseHeaders(404, -1)

            exchange.responseHeaders.apply {
                add("Content-Type", "image/png")
                // A skin id names one texture forever, so the face never changes.
                add("Cache-Control", "max-age=31536000, immutable")
            }
            exchange.sendResponseHeaders(200, png.size.toLong())
            exchange.responseBody.write(png)
        } catch (_: IOException) {
            // Bridge went away.
        } finally {
            exchange.close()
        }
    }

    companion object {
        /** Hash bytes used for the icon ETag. */
        private const val ETAG_BYTES = 8

        /** Key of the default icon, served when [Host.iconKey] names none. */
        const val DEFAULT_ICON = ""

        /** Frames buffered per bridge before it is considered stuck. Bursts such as a proxy restart are
         * emptying a large server queue one frame per player, so leave plenty of room. */
        private const val QUEUE_CAPACITY = 1024

        /** Seconds between keep-alive comments on an idle stream. */
        private const val HEARTBEAT_SECONDS = 15L

        /** Largest accepted request body. */
        private const val MAX_REQUEST_BYTES = 4096

        /** SSE comment sent when nothing happened for a while. */
        private const val HEARTBEAT = ": ping\n\n"

        /** Sentinel telling a handler to finish; compared by identity. */
        private val CLOSE = String(CharArray(0))
    }
}
