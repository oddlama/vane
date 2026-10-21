package org.oddlama.vane.proxycore

import java.net.SocketAddress
import java.util.*

/** Represents a login connection before the player is fully connected. */
interface ProxyPendingConnection {
    /** Name provided during login. */
    val name: String?

    /** UUID associated with the connection, if available at this stage. */
    val uniqueId: UUID?

    /** Remote port used by the client to connect. */
    val port: Int

    /** Raw remote socket address of the client. */
    val socketAddress: SocketAddress?

    /**
     * Checks whether this connection has at least one permission in [permission].
     *
     * @param server proxy server context.
     * @param permission permissions to verify.
     * @return `true` when the connection is authorized.
     */
    fun hasPermission(server: ProxyServer?, vararg permission: String?): Boolean

    /**
     * Checks whether this connection may trigger startup for [serverName].
     *
     * @param server proxy server context.
     * @param serverName managed server identifier.
     * @return `true` when startup is permitted.
     */
    fun canStartServer(server: ProxyServer?, serverName: String?): Boolean =
        hasPermission(
            server,
            "vane_proxy.start_server",
            "vane_proxy.start_server.*",
            "vane_proxy.start_server.$serverName",
        )
}
