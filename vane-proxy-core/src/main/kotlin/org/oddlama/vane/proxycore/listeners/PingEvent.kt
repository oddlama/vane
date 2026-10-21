package org.oddlama.vane.proxycore.listeners

import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo

/**
 * Base ping event that applies configured MOTD and favicon.
 *
 * @property plugin owning plugin instance.
 * @property ping mutable ping response wrapper.
 * @property server backend server currently being pinged.
 */
abstract class PingEvent(
    var plugin: VaneProxyPlugin,
    @JvmField var ping: ProxyServerPing,
    var server: IVaneProxyServerInfo,
) : ProxyEvent {
    /** Applies presentation values and sends the ping response. */
    override fun fire() {
        ping.setDescription(plugin.getMotd(server))
        ping.setFavicon(plugin.getFavicon(server))

        sendResponse()
    }

    /** Dispatches the platform-specific ping response. */
    abstract fun sendResponse()
}
