package org.oddlama.vane.proxycore

import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler
import java.util.*

/**
 * Abstraction of a proxy runtime used by [VaneProxyPlugin].
 */
interface ProxyServer {
    /** Scheduler used for asynchronous and delayed tasks. */
    val scheduler: ProxyTaskScheduler?

    /**
     * Broadcasts a message to all connected players.
     *
     * @param message formatted message to broadcast.
     */
    fun broadcast(message: String?)

    /** Collection of currently connected players. */
    val players: MutableCollection<ProxyPlayer?>?

    /**
     * Checks whether the subject represented by [uuid] has at least one of the provided permissions.
     *
     * @param uuid player UUID to check.
     * @param permission permissions to test.
     * @return `true` when permission checks pass.
     */
    fun hasPermission(uuid: UUID?, vararg permission: String?): Boolean

    /**
     * Checks whether a backend called [name] is registered with the proxy.
     *
     * @param name backend server name.
     * @return `true` when the proxy knows the server.
     */
    fun hasServer(name: String): Boolean

    /**
     * Returns the backend [player] is connected to.
     *
     * @param player player UUID.
     * @return the backend's name, or `null` when the player is offline or between servers.
     */
    fun serverOf(player: UUID): String?

    /** Maximum player count shown in the server list. */
    val maxPlayers: Int

    /** The proxy's server-list icon as a `data:image/png;base64,` URL, or `null` without one. */
    val favicon: String?

    /** Whether the proxy authenticates players with Mojang, so their connection is encrypted. */
    val onlineMode: Boolean
}
