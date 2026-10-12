package org.oddlama.vane.busybar

import org.bukkit.permissions.Permission
import org.json.JSONObject
import org.oddlama.vane.busybar.common.BridgeServer
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import java.io.IOException
import java.net.InetSocketAddress
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * The [BridgeServer] that bridges connect to, serving HTTPS unless [Tls] is off. See the module
 * documentation for its endpoints.
 *
 * Behind a proxy (see [org.oddlama.vane.busybar.link.ProxyLink]) the proxy serves the bridges
 * instead, so the server is not started; events still go to the proxy through [emit].
 *
 * @param context owning context.
 */
class EventStream(context: Context<BusyBar?>) : ModuleComponent<BusyBar?>(context), BridgeServer.Host {
    private val busybar: BusyBar
        get() = requireNotNull(module)

    /** The server, created when enabling. */
    private var server: BridgeServer? = null

    /** Whether `server.stop` was already sent during this shutdown. */
    private var stopAnnounced = false

    /** ETag of the server icon, or null when the server has none. Bridges compare it to their cached copy. */
    fun iconTag(): String? = server?.iconTag()

    /** Starts the server, unless the proxy serves the bridges. */
    override fun onEnable() {
        stopAnnounced = false
        if (busybar.proxied()) {
            busybar.log.info("BUSY Bar bridges are served by the proxy, so this server's event stream stays off.")
            return
        }
        server = BridgeServer(this, busybar.heads, busybar.bridgeLog).apply { setIcon(loadIcon()) }
        startServer()
    }

    /** Tells connected bridges the server is going away, then stops the server. */
    override fun onDisable() {
        announceStop()
        server?.stop()
        server = null
    }

    /** Restarts the server with freshly loaded TLS settings, disconnecting every bridge. Call from the main thread. */
    fun restart() {
        server?.stop()
        startServer()
    }

    /** Binds the server, with TLS unless it is off. Logs and stays stopped on failure. */
    private fun startServer() {
        val current = server ?: return
        val address = InetSocketAddress(busybar.configBindAddress, busybar.configPort)
        try {
            val ssl = busybar.tls.load()
            current.start(address, ssl)
            val protocol = if (ssl == null) "HTTP" else "HTTPS"
            busybar.log.info("BUSY Bar event stream listening on $address ($protocol, Tls ${busybar.tls.mode.name.lowercase()})")
        } catch (e: IOException) {
            busybar.log.log(Level.SEVERE, "Failed to start the BUSY Bar event stream on $address", e)
        } catch (e: GeneralSecurityException) {
            busybar.log.log(
                Level.SEVERE,
                "Failed to set up TLS for the BUSY Bar event stream. With Tls auto, delete tls-auto.key and " +
                    "tls-auto.crt from the plugin folder to generate a new certificate; with Tls keystore, check " +
                    "TlsKeystore and TlsKeystorePassword.",
                e
            )
        }
    }

    /**
     * Queues an event for connected bridges and forwards it to the proxy, if linked. Call from the
     * main thread.
     *
     * @param type event name, for example `player.join`.
     * @param data event payload.
     * @param target only deliver to this player's bridges, or to everyone when null.
     * @param permission only deliver to bridges whose owner holds this permission, or to everyone when null.
     */
    fun emit(type: String, data: JSONObject, target: UUID? = null, permission: Permission? = null) {
        busybar.proxyLink.forward(type, data, target, permission)
        server?.emit(type, data) { player ->
            (target == null || player == target) && (permission == null || busybar.access.has(player, permission))
        }
    }

    /** Sends `server.stop` once per shutdown, from whichever comes first: the shutdown kicks or disabling. */
    fun announceStop() {
        if (stopAnnounced) return
        stopAnnounced = true
        emit("server.stop", JSONObject().put("reason", "shutdown"))
    }

    /** Closes every bridge connection of [player]. */
    fun disconnect(player: UUID) {
        server?.disconnect(player)
    }

    /** Number of bridges currently connected for [player]. */
    fun connectedCount(player: UUID): Int = server?.connectedCount(player) ?: 0

    override fun playerForToken(token: String): UUID? = busybar.playerForToken(token)

    /** Builds the snapshot on the main thread, waiting up to [SYNC_TIMEOUT_SECONDS]. */
    override fun snapshot(player: UUID): JSONObject? = try {
        busybar.server.scheduler.callSyncMethod(busybar) { busybar.snapshot(player) }
            .get(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } catch (_: Exception) {
        null
    }

    /** Checks the request against the configuration and permissions, then runs it on the next tick. */
    override fun request(player: UUID, request: JSONObject): Int {
        val action = when (request.optString("type")) {
            "presence.set" -> "presence.set"
            "action.invoke" -> request.optString("action")
            else -> return 400
        }
        val permission = busybar.permissionForAction(action) ?: return 400
        if (!busybar.actionAllowed(action) || !busybar.access.has(player, permission)) return 403
        busybar.scheduleNextTick { busybar.handleRequest(player, request) }
        return 202
    }

    /** Decodes the server icon Paper loaded from `server-icon.png`. Call from the main thread. */
    private fun loadIcon(): ByteArray? {
        val data = busybar.server.serverIcon?.data ?: return null
        return try {
            Base64.getDecoder().decode(data.substringAfter("base64,"))
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    companion object {
        /** Seconds to wait for the main thread to build a snapshot. */
        private const val SYNC_TIMEOUT_SECONDS = 5L
    }
}
