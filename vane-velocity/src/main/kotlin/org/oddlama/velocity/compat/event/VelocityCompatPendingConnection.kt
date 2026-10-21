package org.oddlama.velocity.compat.event

import com.velocitypowered.api.proxy.InboundConnection
import com.velocitypowered.api.proxy.Player
import java.net.SocketAddress
import java.util.*
import org.oddlama.vane.proxycore.ProxyPendingConnection
import org.oddlama.vane.proxycore.ProxyServer

/**
 * Velocity implementation of proxy-core pending connection abstraction.
 *
 * @property connection wrapped inbound connection.
 * @property name pending username, when available.
 * @property uniqueId player UUID, when available.
 */
class VelocityCompatPendingConnection
private constructor(
    private val connection: InboundConnection,
    override val name: String?,
    override val uniqueId: UUID?,
) : ProxyPendingConnection {
    /**
     * Creates a pending connection wrapper from a generic inbound connection.
     *
     * @param connection inbound connection.
     * @param username requested username for login.
     */
    constructor(connection: InboundConnection, username: String?) : this(connection, username, null)

    /**
     * Creates a pending connection wrapper from a logged-in Velocity player.
     *
     * @param player Velocity player.
     */
    constructor(player: Player) : this(player, player.username, player.uniqueId)

    /** Virtual host port used by the inbound connection. */
    override val port: Int
        get() = connection.virtualHost.orElseThrow().port

    /** Virtual host socket address used by the inbound connection. */
    override val socketAddress: SocketAddress
        get() = connection.virtualHost.orElseThrow()

    /**
     * Checks whether the wrapped player has any provided permission.
     *
     * @param server unused for Velocity permission checks.
     * @param permission candidate permissions.
     * @return `true` when the wrapped connection is a player with at least one permission.
     */
    override fun hasPermission(server: ProxyServer?, vararg permission: String?): Boolean {
        val player = connection as? Player ?: return false
        return permission.filterNotNull().any(player::hasPermission)
    }
}
