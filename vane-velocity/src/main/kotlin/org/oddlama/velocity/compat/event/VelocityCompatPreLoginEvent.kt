package org.oddlama.velocity.compat.event

import com.velocitypowered.api.event.connection.PreLoginEvent
import java.util.logging.Level
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.oddlama.vane.proxycore.ProxyPendingConnection
import org.oddlama.vane.proxycore.VaneProxyPlugin

/**
 * Velocity-backed implementation of proxy-core pre-login event abstraction.
 *
 * @param plugin plugin handling this event.
 * @property event wrapped Velocity pre-login event.
 */
class VelocityCompatPreLoginEvent(
    plugin: VaneProxyPlugin,
    val event: PreLoginEvent,
) : org.oddlama.vane.proxycore.listeners.PreLoginEvent(plugin) {
    /** Cancels with a default deny reason. */
    override fun cancel() {
        cancel(null)
    }

    /**
     * Cancels login and sets a denial component.
     *
     * @param reason deny reason shown to the client and logs.
     */
    override fun cancel(reason: String?) {
        val resolvedReason = reason.orEmpty()

        plugin
            .getLogger()
            .log(
                Level.WARNING,
                "Denying multiplexer connection from " +
                    event.connection.remoteAddress +
                    ": " +
                    resolvedReason.ifEmpty { "No reason provided" },
            )

        event.result =
            PreLoginEvent.PreLoginComponentResult.denied(
                LegacyComponentSerializer.legacySection()
                    .deserialize(
                        resolvedReason.ifEmpty { "Failed to authorize multiplexer connection" }
                    )
            )
    }

    /** Wrapped pending connection for proxy-core logic. */
    override val connection: ProxyPendingConnection
        get() = VelocityCompatPendingConnection(event.connection, event.username)

    /**
     * Velocity-specific multiplex auth is completed later during game profile request handling.
     *
     * @param multiplexedPlayer multiplexed player candidate.
     * @return always `true` for this stage on Velocity.
     */
    override fun implementationSpecificAuth(multiplexedPlayer: MultiplexedPlayer?): Boolean {
        // Not applicable, all handled in `ProxyGameProfileRequestListener`
        return true
    }
}
