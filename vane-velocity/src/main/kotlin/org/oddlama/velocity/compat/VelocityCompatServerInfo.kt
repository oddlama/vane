package org.oddlama.velocity.compat

import com.velocitypowered.api.proxy.server.RegisteredServer
import java.net.SocketAddress
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo
import org.oddlama.velocity.Velocity

/**
 * Velocity implementation of proxy-core server info abstraction.
 *
 * @property server wrapped Velocity registered server.
 */
class VelocityCompatServerInfo(private val server: RegisteredServer) : IVaneProxyServerInfo {
    /** Backend server name. */
    override val name: String?
        get() = server.serverInfo.name

    /** Backend network address. */
    override val socketAddress: SocketAddress?
        get() = server.serverInfo.address

    /**
     * Sends plugin message data to the backend server.
     *
     * @param data payload bytes.
     */
    override fun sendData(data: ByteArray?) {
        if (data == null) return
        server.sendPluginMessage(Velocity.CHANNEL, data)
    }

    /**
     * Queue-aware plugin messaging is not supported on Velocity.
     *
     * @param data payload bytes.
     * @param queue ignored queue flag.
     * @return always `false` because queueing is unavailable.
     */
    override fun sendData(data: ByteArray?, queue: Boolean): Boolean {
        // Velocity does not expose queueing semantics for plugin messages.
        return false
    }
}
