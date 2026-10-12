package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import org.oddlama.vane.proxycore.ProxyPlayer
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler
import org.oddlama.velocity.compat.scheduler.VelocityCompatProxyTaskScheduler
import java.util.*

/**
 * Velocity implementation of the proxy-core server abstraction.
 *
 * @property proxy wrapped Velocity proxy server.
 */
class VelocityCompatProxyServer(private val proxy: ProxyServer) : org.oddlama.vane.proxycore.ProxyServer {
    /**
     * Scheduler adapter exposing Velocity scheduling through proxy-core interfaces.
     */
    override val scheduler: ProxyTaskScheduler
        get() = VelocityCompatProxyTaskScheduler(proxy.scheduler)

    /**
     * Broadcasts a plain-text message to all connected players.
     *
     * @param message message to broadcast.
     */
    override fun broadcast(message: String?) {
        proxy.sendMessage(Component.text(message.orEmpty()))
    }

    /**
     * Snapshot of currently connected players wrapped as proxy-core players.
     */
    override val players: MutableCollection<ProxyPlayer?>
        get() = proxy.allPlayers.mapTo(mutableListOf()) { VelocityCompatProxyPlayer(it) as ProxyPlayer? }

    /**
     * Checks whether a specific player has any of the given permissions.
     *
     * @param uuid target player UUID.
     * @param permission candidate permissions to evaluate.
     * @return `true` when the player exists and has at least one permission.
     */
    override fun hasPermission(uuid: UUID?, vararg permission: String?): Boolean {
        if (uuid == null) return false
        return proxy.getPlayer(uuid)
            .map { player -> permission.filterNotNull().any(player::hasPermission) }
            .orElse(false)
    }

    /**
     * Checks whether a backend called [name] is registered in Velocity.
     *
     * @param name backend server name.
     * @return `true` when Velocity knows the server.
     */
    override fun hasServer(name: String): Boolean = proxy.getServer(name).isPresent

    /**
     * Name of the backend [player] is connected to.
     *
     * @param player player UUID.
     * @return the backend's name, or `null` when offline or between servers.
     */
    override fun serverOf(player: UUID): String? =
        proxy.getPlayer(player).flatMap { it.currentServer }.map { it.serverInfo.name }.orElse(null)

    /** Maximum player count from velocity.toml. */
    override val maxPlayers: Int
        get() = proxy.configuration.showMaxPlayers

    /** The proxy's favicon as a data URL. */
    override val favicon: String?
        get() = proxy.configuration.favicon.map { it.base64Url }.orElse(null)

    /** Whether velocity.toml enables online mode. */
    override val onlineMode: Boolean
        get() = proxy.configuration.isOnlineMode
}
