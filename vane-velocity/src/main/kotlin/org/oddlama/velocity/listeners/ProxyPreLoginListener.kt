package org.oddlama.velocity.listeners

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PreLoginEvent
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.listeners.PreLoginEvent.PreLoginDestination
import org.oddlama.velocity.compat.event.VelocityCompatPreLoginEvent

/**
 * Handles Velocity pre-login events and forwards them to proxy-core pre-login logic.
 *
 * @property plugin plugin instance used to construct proxy-core event wrappers.
 */
class ProxyPreLoginListener @Inject constructor(private val plugin: VaneProxyPlugin) {
    /**
     * Fires proxy-core pre-login processing for incoming connections.
     *
     * @param event Velocity pre-login event.
     */
    @Subscribe
    fun preLogin(event: PreLoginEvent) {
        val proxyEvent: org.oddlama.vane.proxycore.listeners.PreLoginEvent =
            VelocityCompatPreLoginEvent(plugin, event)

        // For Velocity, our multiplexer connections need more work; they
        // later get handled in `ProxyGameProfileRequestListener`
        proxyEvent.fire(PreLoginDestination.PENDING_MULTIPLEXED_LOGINS)
    }
}
