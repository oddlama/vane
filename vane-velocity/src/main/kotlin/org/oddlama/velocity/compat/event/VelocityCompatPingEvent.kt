package org.oddlama.velocity.compat.event

import com.velocitypowered.api.event.proxy.ProxyPingEvent
import org.oddlama.vane.proxycore.ProxyPendingConnection
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo
import org.oddlama.vane.proxycore.listeners.PingEvent
import org.oddlama.velocity.compat.VelocityCompatProxyServerPing

/**
 * Velocity-backed implementation of proxy-core ping event abstraction.
 *
 * @param plugin plugin handling this event.
 * @property event wrapped Velocity ping event.
 * @param server target server information for ping processing.
 */
class VelocityCompatPingEvent(
    plugin: VaneProxyPlugin,
    val event: ProxyPingEvent,
    server: IVaneProxyServerInfo,
) :
    PingEvent(
        plugin,
        VelocityCompatProxyServerPing(event.ping),
        server,
    ) {
    /** Writes the mutated ping response back into the wrapped Velocity event. */
    override fun sendResponse() {
        event.ping = (ping as VelocityCompatProxyServerPing).builder.build()
    }

    /** Ping requests do not expose a proxy-core pending connection in Velocity. */
    override val connection: ProxyPendingConnection?
        get() = null
}
