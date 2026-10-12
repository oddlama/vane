package org.oddlama.vane.proxycore.busybar

import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.busybar.common.LinkProtocol
import org.oddlama.vane.proxycore.busybar.BackendLink.Companion.toUuidOrNull
import org.oddlama.vane.proxycore.log.IVaneLogger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * Accepts connections from vane-busybar backends, so that the proxy can serve BUSY Bar bridges for
 * the whole network.
 *
 * Each backend opens one TCP connection carrying [LinkProtocol] frames. The handshake proves to
 * both sides that the other knows [secret], without sending it:
 *
 * ```
 * proxy   → {"t":"challenge","proto":1,"nonce":"…"}
 * backend → {"t":"hello","proto":1,"server":"survival","version":"…","caps":[…],"nonce":"…","mac":"…"}
 * proxy   → {"t":"welcome","mac":"…","busy":["<uuid>",…],"bridges":["<uuid>",…]}
 * ```
 *
 * A backend may only use a server name [isKnownServer] accepts; a second connection under the same
 * name replaces the first. Afterwards backends send `event`, `grants`, `grants.bulk`,
 * `snapshot.res`, `action.res`, `bye` and `pong`; the proxy sends `snapshot.req`, `action`,
 * `presence`, `bridge` and `ping`. See [BackendLink] for the requests.
 *
 * @param logger proxy logger.
 * @param secret shared with every backend's `ProxyLinkSecret`, at least [LinkProtocol.MIN_SECRET_LENGTH] long.
 * @param isKnownServer whether a backend name is registered with the proxy.
 * @param handler receives what backends report.
 */
class BackendLinkServer(
    private val logger: IVaneLogger,
    private val secret: String,
    private val isKnownServer: (String) -> Boolean,
    private val handler: BackendLinkHandler
) {
    /** Linked backends by server name. */
    private val links = ConcurrentHashMap<String, BackendLink>()

    @Volatile
    private var serverSocket: ServerSocket? = null
    private var connections: ExecutorService? = null
    private var pinger: ScheduledExecutorService? = null

    /** The backend linked under [server], if any. */
    fun link(server: String): BackendLink? = links[server]

    /** Every linked backend. */
    fun links(): Collection<BackendLink> = links.values

    /**
     * Starts listening on [address].
     *
     * @throws IOException when the address cannot be bound.
     */
    fun start(address: InetSocketAddress) {
        val socket = ServerSocket().apply { bind(address) }
        val exec = Executors.newVirtualThreadPerTaskExecutor()
        serverSocket = socket
        connections = exec
        pinger = Executors.newSingleThreadScheduledExecutor { Thread.ofVirtual().unstarted(it) }.apply {
            scheduleAtFixedRate(
                { links.values.forEach { it.send(JSONObject().put("t", "ping")) } },
                LinkProtocol.PING_INTERVAL_MS, LinkProtocol.PING_INTERVAL_MS, TimeUnit.MILLISECONDS
            )
        }
        exec.execute { accept(socket, exec) }
        logger.log(Level.INFO, "BUSY Bar backend link listening on $address")
    }

    /** Closes every link and stops listening. */
    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: IOException) {
        }
        serverSocket = null
        pinger?.shutdownNow()
        pinger = null
        links.values.forEach(BackendLink::close)
        connections?.shutdownNow()
        connections = null
    }

    /** Accepts connections until [socket] closes. */
    private fun accept(socket: ServerSocket, exec: ExecutorService) {
        while (!socket.isClosed) {
            val connection = try {
                socket.accept()
            } catch (_: IOException) {
                return
            }
            exec.execute { serve(connection) }
        }
    }

    /** Runs the handshake, then reads frames until the connection ends. */
    private fun serve(socket: Socket) {
        val remote = socket.remoteSocketAddress
        var link: BackendLink? = null
        var reason = "closed"
        try {
            socket.tcpNoDelay = true
            socket.soTimeout = LinkProtocol.HANDSHAKE_TIMEOUT_MS.toInt()
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())
            link = handshake(socket, input, output) ?: return
            // The replaced link's reader then fails and reports its own disconnect as `replaced`.
            links.put(link.server, link)?.close()
            logger.log(Level.INFO, "BUSY Bar backend '${link.server}' linked from $remote")
            handler.onConnect(link)

            socket.soTimeout = LinkProtocol.TIMEOUT_MS.toInt()
            while (true) {
                val frame = LinkProtocol.read(input) ?: break
                if (frame.optString("t") == "bye") {
                    reason = "bye: ${frame.optString("reason")}"
                    break
                }
                dispatch(link, frame)
            }
        } catch (_: SocketTimeoutException) {
            reason = "timeout"
        } catch (e: IOException) {
            reason = e.message ?: "closed"
        } finally {
            try {
                socket.close()
            } catch (_: IOException) {
            }
            if (link != null) {
                link.close()
                reason = when {
                    !links.remove(link.server, link) -> "replaced"
                    serverSocket == null -> "proxy stopping"
                    else -> reason
                }
                logger.log(Level.INFO, "BUSY Bar backend '${link.server}' unlinked ($reason)")
                handler.onDisconnect(link, reason)
            }
        }
    }

    /**
     * Authenticates a new connection. Answers with an `error` frame and returns null when the
     * backend fails.
     */
    private fun handshake(socket: Socket, input: InputStream, output: BufferedOutputStream): BackendLink? {
        val nonce = LinkProtocol.nonce()
        LinkProtocol.write(output, JSONObject().put("t", "challenge").put("proto", LinkProtocol.VERSION).put("nonce", nonce))

        val hello = LinkProtocol.read(input) ?: return null
        val server = hello.optString("server")
        fun reject(reason: String): BackendLink? {
            logger.log(Level.WARNING, "Rejected BUSY Bar backend link from ${socket.remoteSocketAddress}: $reason")
            LinkProtocol.write(output, JSONObject().put("t", "error").put("reason", reason))
            return null
        }
        if (hello.optString("t") != "hello") return reject("expected hello")
        if (hello.optInt("proto") != LinkProtocol.VERSION) {
            return reject("protocol ${hello.optInt("proto")} is not supported, this proxy speaks ${LinkProtocol.VERSION}")
        }
        // Checked before the name, so a wrong secret never reveals which names exist.
        if (!LinkProtocol.macMatches(LinkProtocol.mac(secret, "backend", nonce, server), hello.optString("mac", null))) {
            return reject("wrong secret")
        }
        if (!isKnownServer(server)) return reject("'$server' is not a server registered with the proxy")
        val backendNonce = hello.optString("nonce").takeIf { it.isNotEmpty() } ?: return reject("missing nonce")

        val caps = hello.optJSONArray("caps") ?: JSONArray()
        val link = BackendLink(server, (0 until caps.length()).map(caps::optString).toSet(), socket, output)
        link.send(
            JSONObject()
                .put("t", "welcome")
                .put("mac", LinkProtocol.mac(secret, "proxy", backendNonce, server))
                .put("busy", JSONArray(handler.busyPlayers().map(Any::toString)))
                .put("bridges", JSONArray(handler.playersWithBridges().map(Any::toString)))
        )
        return link
    }

    /** Handles one frame after the handshake. */
    private fun dispatch(link: BackendLink, frame: JSONObject) {
        when (frame.optString("t")) {
            "event" -> parseEvent(link, frame)?.let { handler.onEvent(link, it) }
            "grants", "grants.bulk" -> handler.onGrants(link, BackendLink.parseGrants(frame))
            "snapshot.res", "action.res" -> link.answer(frame)
            "pong" -> {}
            else -> logger.log(Level.FINE, "Ignoring unknown frame '${frame.optString("t")}' from '${link.server}'")
        }
    }

    /** Reads an `event` frame, adding the backend's name to its payload. Null when malformed. */
    private fun parseEvent(link: BackendLink, frame: JSONObject): BackendLinkHandler.Event? {
        val type = frame.optString("type").takeIf { it.isNotEmpty() } ?: return null
        val to = frame.optJSONObject("to") ?: return null
        val scope = when (to.optString("scope")) {
            "all" -> BackendLinkHandler.Scope.ALL
            "server" -> BackendLinkHandler.Scope.SERVER
            "player" -> BackendLinkHandler.Scope.PLAYER
            else -> return null
        }
        val player = to.optString("player").toUuidOrNull()
        if (scope == BackendLinkHandler.Scope.PLAYER && player == null) return null
        val data = (frame.optJSONObject("data") ?: JSONObject()).put("server", link.server)
        return BackendLinkHandler.Event(type, data, scope, player, frame.optString("perm").takeIf { it.isNotEmpty() })
    }
}
