package org.oddlama.vane.proxycore.busybar

import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.busybar.common.LinkProtocol
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * One authenticated vane-busybar backend, see [BackendLinkServer].
 *
 * @property server the backend's name as registered with the proxy.
 * @property capabilities optional integrations the backend has, such as `regions`.
 */
class BackendLink internal constructor(
    val server: String,
    val capabilities: Set<String>,
    private val socket: Socket,
    private val output: OutputStream
) {
    /** Requests waiting for their answer, by id. */
    private val pending = ConcurrentHashMap<Long, CompletableFuture<JSONObject>>()

    private val nextId = AtomicLong()

    /** Whether the link is still open. */
    @Volatile
    var open = true
        private set

    /**
     * Asks the backend for its part of [player]'s bridge snapshot. Completes exceptionally after
     * [REQUEST_TIMEOUT_MS], when the backend reports an error, or when the link closes.
     */
    fun requestSnapshot(player: UUID): CompletableFuture<JSONObject> =
        request(JSONObject().put("t", "snapshot.req").put("player", player.toString()))
            .thenApply { it.getJSONObject("data") }

    /**
     * Asks the backend to run [action], for example `autostop.abort`, on behalf of [player]. [by] is
     * shown in the backend's logs. Completes with whether the backend ran it.
     */
    fun invokeAction(player: UUID, action: String, by: String): CompletableFuture<Boolean> =
        request(
            JSONObject().put("t", "action").put("player", player.toString()).put("action", action).put("by", by)
        ).thenApply { it.optBoolean("ok") }

    /** Tells the backend [player]'s focus mode changed. */
    fun sendPresence(player: UUID, busy: Boolean) =
        send(JSONObject().put("t", "presence").put("player", player.toString()).put("busy", busy))

    /** Tells the backend [player] connected their first bridge, or disconnected their last one. */
    fun sendBridge(player: UUID, connected: Boolean) =
        send(JSONObject().put("t", "bridge").put("player", player.toString()).put("connected", connected))

    /** Sends one frame; closes the link when that fails. Safe to call from any thread. */
    fun send(frame: JSONObject) {
        if (!open) return
        try {
            synchronized(output) { LinkProtocol.write(output, frame) }
        } catch (_: IOException) {
            close()
        }
    }

    /** Sends a request carrying a fresh id and returns the matching answer. */
    private fun request(frame: JSONObject): CompletableFuture<JSONObject> {
        val id = nextId.incrementAndGet()
        val future = CompletableFuture<JSONObject>().orTimeout(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        future.whenComplete { _, _ -> pending.remove(id) }
        pending[id] = future
        send(frame.put("id", id))
        if (!open) future.completeExceptionally(IOException("Link to $server is closed"))
        return future
    }

    /** Completes the request an answer frame belongs to. */
    internal fun answer(frame: JSONObject) {
        val future = pending[frame.optLong("id", -1)] ?: return
        if (frame.has("error")) future.completeExceptionally(IOException(frame.optString("error")))
        else future.complete(frame)
    }

    /** Closes the socket and fails every pending request. */
    internal fun close() {
        open = false
        try {
            socket.close()
        } catch (_: IOException) {
        }
        val failure = IOException("Link to $server closed")
        pending.values.forEach { it.completeExceptionally(failure) }
    }

    /**
     * Short permission names a backend reports [player] holds there; [online] when [player] is on
     * that backend right now.
     */
    data class Grants(val player: UUID, val granted: List<String>, val online: Boolean)

    companion object {
        /** Milliseconds a backend has to answer a request. */
        const val REQUEST_TIMEOUT_MS = 5_000L

        /** Reads a `grants` or `grants.bulk` frame. */
        internal fun parseGrants(frame: JSONObject): List<Grants> {
            val entries = if (frame.optString("t") == "grants") listOf(frame)
            else frame.optJSONArray("grants")?.let { a -> (0 until a.length()).mapNotNull(a::optJSONObject) }.orEmpty()
            return entries.mapNotNull { entry ->
                val player = entry.optString("player").toUuidOrNull() ?: return@mapNotNull null
                val granted = entry.optJSONArray("granted") ?: JSONArray()
                Grants(player, (0 until granted.length()).map(granted::optString), entry.optBoolean("online"))
            }
        }

        /** Parses a UUID, or returns null. */
        internal fun String.toUuidOrNull(): UUID? = try {
            UUID.fromString(this)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
