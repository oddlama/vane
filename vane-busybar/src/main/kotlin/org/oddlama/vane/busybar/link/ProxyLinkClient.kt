package org.oddlama.vane.busybar.link

import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.busybar.common.LinkProtocol
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Keeps one connection to the proxy's BUSY Bar backend link open, reconnecting with backoff.
 * Knows nothing about Bukkit; [ProxyLink] adapts it to the module.
 *
 * Outgoing frames go through a bounded queue drained by a writer thread, so [send] never blocks the
 * caller. While unlinked, frames are dropped: they describe the current state, and [Handler.onLinked]
 * is where a fresh link catches up.
 *
 * @param address the proxy's link address, possibly unresolved.
 * @param server this backend's name as registered with the proxy.
 * @param secret shared with the proxy's `BusyBar.Link.Secret`.
 * @param version plugin version, for the proxy's logs.
 * @param capabilities optional integrations this backend has, such as `regions`.
 * @param handler receives what the proxy sends, on the link's own thread.
 * @param log logger for connection changes.
 */
class ProxyLinkClient(
    private val address: InetSocketAddress,
    private val server: String,
    private val secret: String,
    private val version: String,
    private val capabilities: Set<String>,
    private val handler: Handler,
    private val log: Logger
) {
    /** [address] as configured, without the `/<unresolved>` an unresolved address prints. */
    private val shown = "${address.hostString}:${address.port}"

    /** What the proxy may ask of a backend. Called on the link's thread; must not block for long. */
    interface Handler {
        /**
         * The link was established. [busy] are the players in focus mode, [bridges] the players with
         * a bridge connected to the proxy.
         */
        fun onLinked(busy: Set<UUID>, bridges: Set<UUID>)

        /** The link went away; the client reconnects on its own. */
        fun onUnlinked()

        /** [player] connected their first bridge, or disconnected their last one. */
        fun onBridge(player: UUID, connected: Boolean)

        /** [player]'s focus mode changed. */
        fun onPresence(player: UUID, busy: Boolean)

        /** Builds this backend's part of [player]'s snapshot and passes it to [reply], or null on failure. */
        fun snapshot(player: UUID, reply: (JSONObject?) -> Unit)

        /** Runs [action] for [player]; [by] names who asked. Passes whether it ran to [reply]. */
        fun action(player: UUID, action: String, by: String, reply: (Boolean) -> Unit)
    }

    /** One established connection. */
    private class Session(val socket: Socket) {
        val queue = LinkedBlockingQueue<JSONObject>(QUEUE_CAPACITY + 1)
    }

    @Volatile
    private var running = false

    @Volatile
    private var session: Session? = null

    private var thread: Thread? = null

    /** The last problem logged, so a proxy that stays down is reported once rather than every retry. */
    private var lastProblem: String? = null

    /** Whether the link is established. */
    val linked: Boolean
        get() = session != null

    /** Starts connecting in the background. */
    fun start() {
        running = true
        thread = Thread.ofVirtual().name("vane-busybar proxy link").start(::run)
    }

    /** Says goodbye to the proxy and stops reconnecting. Waits briefly for the goodbye to be written. */
    fun stop(reason: String) {
        running = false
        val current = session
        if (current != null) {
            // The writer sends the goodbye and closes the socket, which ends the reader. Interrupting
            // the reader instead would close the socket before the goodbye is out.
            current.queue.clear()
            current.queue.offer(JSONObject().put("t", "bye").put("reason", reason))
            current.queue.offer(CLOSE)
        } else {
            thread?.interrupt()
        }
        thread?.join(STOP_WAIT_MS)
        session?.socket?.close()
        thread = null
    }

    /**
     * Queues [frame] for the proxy or drops it while unlinked. A proxy that stops reading is
     * disconnected instead of buffering without a bound. Safe to call from any thread.
     */
    fun send(frame: JSONObject) {
        val current = session ?: return
        if (current.queue.size >= QUEUE_CAPACITY || !current.queue.offer(frame)) {
            log.warning("The proxy is not keeping up with BUSY Bar events; reconnecting.")
            current.socket.close()
        }
    }

    /** Connects, serves, and retries with backoff until [stop]. */
    private fun run() {
        var backoff = MIN_BACKOFF_MS
        while (running) {
            try {
                if (connect()) backoff = MIN_BACKOFF_MS
            } catch (e: IOException) {
                problem("Cannot link to the proxy at $shown: ${e.message}. Retrying in the background.")
            }
            if (!running) return
            try {
                Thread.sleep(backoff)
            } catch (_: InterruptedException) {
                return
            }
            backoff = minOf(backoff * 2, MAX_BACKOFF_MS)
        }
    }

    /** One connection from start to end. Returns whether the handshake succeeded. */
    private fun connect(): Boolean {
        Socket().use { socket ->
            // Resolved on every attempt, so a proxy that moves to another address is found again.
            socket.connect(InetSocketAddress(address.hostString, address.port), LinkProtocol.HANDSHAKE_TIMEOUT_MS.toInt())
            socket.tcpNoDelay = true
            socket.soTimeout = LinkProtocol.HANDSHAKE_TIMEOUT_MS.toInt()
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())

            val welcome = handshake(input, output)
            val current = Session(socket)
            val writer = Thread.ofVirtual().name("vane-busybar proxy link writer").start { write(current, output) }
            session = current
            lastProblem = null
            log.info("Linked to the proxy at $shown as '$server'.")
            try {
                handler.onLinked(uuids(welcome.optJSONArray("busy")), uuids(welcome.optJSONArray("bridges")))
                socket.soTimeout = LinkProtocol.TIMEOUT_MS.toInt()
                while (true) {
                    val frame = LinkProtocol.read(input) ?: throw IOException("the proxy closed the link")
                    dispatch(frame)
                }
            } catch (e: IOException) {
                if (running) {
                    val why = if (e is SocketTimeoutException) "no ping for ${LinkProtocol.TIMEOUT_MS / 1000} s" else e.message
                    log.warning("Lost the link to the proxy ($why). Reconnecting.")
                }
            } finally {
                session = null
                writer.interrupt()
                handler.onUnlinked()
            }
            return true
        }
    }

    /** Answers the challenge and checks the proxy's proof. Returns the `welcome` frame. */
    private fun handshake(input: BufferedInputStream, output: BufferedOutputStream): JSONObject {
        val challenge = LinkProtocol.read(input) ?: throw IOException("the proxy closed the link")
        if (challenge.optString("t") != "challenge") throw IOException("expected a challenge, is $shown the BusyBar.Link port?")
        if (challenge.optInt("proto") != LinkProtocol.VERSION) {
            throw IOException("the proxy speaks protocol ${challenge.optInt("proto")}, this plugin ${LinkProtocol.VERSION}; update both to the same vane version")
        }
        val nonce = LinkProtocol.nonce()
        LinkProtocol.write(
            output,
            JSONObject()
                .put("t", "hello")
                .put("proto", LinkProtocol.VERSION)
                .put("server", server)
                .put("version", version)
                .put("caps", JSONArray(capabilities.sorted()))
                .put("nonce", nonce)
                .put("mac", LinkProtocol.mac(secret, "backend", challenge.optString("nonce"), server))
        )

        val welcome = LinkProtocol.read(input) ?: throw IOException("the proxy closed the link")
        if (welcome.optString("t") == "error") throw IOException("the proxy refused: ${welcome.optString("reason")}")
        if (welcome.optString("t") != "welcome") throw IOException("expected welcome")
        if (!LinkProtocol.macMatches(LinkProtocol.mac(secret, "proxy", nonce, server), welcome.optString("mac", null))) {
            throw IOException("the proxy could not prove it knows ProxyLinkSecret")
        }
        return welcome
    }

    /** Writes queued frames until the close marker, interruption, or a writing error. */
    private fun write(current: Session, output: BufferedOutputStream) {
        try {
            while (true) {
                val frame = current.queue.take()
                if (frame === CLOSE) break
                LinkProtocol.write(output, frame)
            }
        } catch (_: InterruptedException) {
        } catch (_: IOException) {
        }
        current.socket.close()
    }

    /** Handles one frame from the proxy. */
    private fun dispatch(frame: JSONObject) {
        val player = frame.optString("player").toUuidOrNull()
        val id = frame.optLong("id")
        when (frame.optString("t")) {
            "ping" -> send(JSONObject().put("t", "pong"))
            "presence" -> if (player != null) handler.onPresence(player, frame.optBoolean("busy"))
            "bridge" -> if (player != null) handler.onBridge(player, frame.optBoolean("connected"))
            "snapshot.req" -> {
                if (player == null) return send(error("snapshot.res", id, "invalid player"))
                handler.snapshot(player) { data ->
                    send(
                        if (data == null) error("snapshot.res", id, "snapshot failed")
                        else JSONObject().put("t", "snapshot.res").put("id", id).put("data", data)
                    )
                }
            }
            "action" -> {
                if (player == null) return send(error("action.res", id, "invalid player"))
                handler.action(player, frame.optString("action"), frame.optString("by")) { ok ->
                    send(JSONObject().put("t", "action.res").put("id", id).put("ok", ok))
                }
            }
        }
    }

    /** Logs [message] unless it repeats the last problem. */
    private fun problem(message: String) {
        if (message == lastProblem) return log.log(Level.FINE, message)
        lastProblem = message
        log.warning(message)
    }

    companion object {
        /** Frames buffered before the proxy count as stuck. */
        private const val QUEUE_CAPACITY = 4096

        private const val MIN_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L

        /** Milliseconds [stop] waits for the goodbye. */
        private const val STOP_WAIT_MS = 1_000L

        /** Marker ending the writer; compared by identity. */
        private val CLOSE = JSONObject()

        private fun error(type: String, id: Long, reason: String) =
            JSONObject().put("t", type).put("id", id).put("error", reason)

        /** The valid UUIDs in [array]. */
        private fun uuids(array: JSONArray?): Set<UUID> =
            array?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).toUuidOrNull() }.toSet() }.orEmpty()

        private fun String.toUuidOrNull(): UUID? = try {
            UUID.fromString(this)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
