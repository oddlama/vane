package org.oddlama.velocity

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import java.net.InetSocketAddress

/** Utility helpers for Velocity-specific server selection behavior. */
object Util {
    /**
     * Resolves the backend server for a virtual host using forced-host rules with default fallback.
     *
     * @param proxy proxy server used to resolve server registrations.
     * @param host requested virtual host.
     * @return resolved registered backend server.
     */
    @JvmStatic
    fun getServerForHost(proxy: ProxyServer, host: InetSocketAddress): RegisteredServer {
        val defaultTarget =
            proxy.configuration.attemptConnectionOrder.firstOrNull()
                ?: error("Velocity attemptConnectionOrder must contain at least one target")
        val forcedTarget = proxy.configuration.forcedHosts[host.hostString]?.firstOrNull().orEmpty()
        val target = forcedTarget.ifEmpty { defaultTarget }

        return proxy.getServer(target).orElseGet {
            proxy.getServer(defaultTarget).orElseThrow {
                IllegalStateException(
                    "No registered server found for '$target' or default '$defaultTarget'"
                )
            }
        }
    }
}
