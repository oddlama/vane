package org.oddlama.velocity.listeners

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import org.oddlama.velocity.Util.getServerForHost
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatServerInfo
import org.oddlama.velocity.compat.event.VelocityCompatLoginEvent
import org.oddlama.velocity.compat.event.VelocityCompatPendingConnection

/**
 * Handles player login events and forwards them to proxy-core login checks.
 *
 * @property velocity plugin instance used for proxy and config access.
 */
class ProxyLoginListener @Inject constructor(private val velocity: Velocity) {
    /**
     * Processes a Velocity login event when the current login result is still allowed.
     *
     * @param event Velocity login event.
     */
    @Subscribe(priority = 0)
    fun login(event: LoginEvent) {
        if (!event.result.isAllowed) return

        val proxy = velocity.rawProxy

        val virtualHost = event.player.virtualHost.orElse(null) ?: return

        val server = getServerForHost(proxy, virtualHost)

        val serverInfo = VelocityCompatServerInfo(server)
        val proxyEvent =
            VelocityCompatLoginEvent(
                event,
                velocity,
                serverInfo,
                VelocityCompatPendingConnection(event.player),
            )
        proxyEvent.fire()
    }
}
