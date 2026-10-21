package org.oddlama.velocity.listeners

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import org.oddlama.vane.proxycore.listeners.PingEvent
import org.oddlama.velocity.Util.getServerForHost
import org.oddlama.velocity.Velocity
import org.oddlama.velocity.compat.VelocityCompatServerInfo
import org.oddlama.velocity.compat.event.VelocityCompatPingEvent

/**
 * Handles server list ping requests and routes them through proxy-core ping processing.
 *
 * @property velocity plugin instance used for proxy and config access.
 */
class ProxyPingListener(private val velocity: Velocity) {
    /**
     * Processes a Velocity ping event and applies Vane ping customizations.
     *
     * @param event Velocity ping event.
     */
    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) {
        val proxy = velocity.rawProxy

        val virtualHost = event.connection.virtualHost.orElse(null)

        val server =
            if (virtualHost != null) {
                getServerForHost(proxy, virtualHost)
            } else {
                val defaultTarget =
                    proxy.configuration.attemptConnectionOrder.firstOrNull() ?: return
                proxy.getServer(defaultTarget).orElseThrow {
                    IllegalStateException("No registered server found for default '$defaultTarget'")
                }
            }

        val serverInfo = VelocityCompatServerInfo(server)
        val proxyEvent: PingEvent = VelocityCompatPingEvent(velocity, event, serverInfo)
        proxyEvent.fire()
    }
}
