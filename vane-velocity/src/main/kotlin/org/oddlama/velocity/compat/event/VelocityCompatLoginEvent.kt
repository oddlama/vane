package org.oddlama.velocity.compat.event

import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.connection.LoginEvent
import net.kyori.adventure.text.Component
import org.oddlama.vane.proxycore.ProxyPendingConnection
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo

/**
 * Velocity-backed implementation of proxy-core login event abstraction.
 *
 * @property event wrapped Velocity login event.
 * @param plugin plugin handling this event.
 * @param serverInfo target server information.
 * @property connection wrapped pending connection.
 */
class VelocityCompatLoginEvent(
    val event: LoginEvent,
    plugin: VaneProxyPlugin,
    serverInfo: IVaneProxyServerInfo,
    override var connection: ProxyPendingConnection,
) : org.oddlama.vane.proxycore.listeners.LoginEvent(plugin, serverInfo, connection) {
    /** Cancels with an empty reason. */
    override fun cancel() {
        cancel(null)
    }

    // Implement the nullable signature expected by the base class
    /**
     * Cancels login and sets a deny component.
     *
     * @param reason deny reason shown to the player.
     */
    override fun cancel(reason: String?) {
        event.result = ResultedEvent.ComponentResult.denied(Component.text(reason.orEmpty()))
    }
}
