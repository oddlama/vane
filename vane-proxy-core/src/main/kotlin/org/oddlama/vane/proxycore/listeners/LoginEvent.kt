package org.oddlama.vane.proxycore.listeners

import java.util.*
import java.util.logging.Level
import org.oddlama.vane.proxycore.Maintenance
import org.oddlama.vane.proxycore.ProxyPendingConnection
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo
import org.oddlama.vane.proxycore.config.ManagedServer

/**
 * Handles pre-join login checks and optional backend startup.
 *
 * @property plugin owning plugin instance.
 * @property serverInfo target backend server.
 * @property connection pending player connection.
 */
abstract class LoginEvent(
    var plugin: VaneProxyPlugin,
    var serverInfo: IVaneProxyServerInfo,
    override var connection: ProxyPendingConnection,
) : ProxyEvent, ProxyCancellableEvent {
    /** Executes login flow checks and cancellation/startup actions. */
    override fun fire() {
        val connectionUuid: UUID = checkNotNull(connection.uniqueId)

        val uuid = plugin.multiplexedUuids.getOrDefault(connectionUuid, connectionUuid)

        if (!plugin.canJoinMaintenance(uuid)) {
            this.cancel(plugin.maintenance.formatMessage(Maintenance.MESSAGE_CONNECT))
            return
        }

        plugin
            .getLogger()
            .log(
                Level.INFO,
                "Connection '${connection.name}' is connecting to '${serverInfo.name}'",
            )

        // Start server if necessary
        if (!plugin.isOnline(serverInfo)) {
            val cms: ManagedServer =
                plugin.config.managedServers[serverInfo.name]
                    ?: run {
                        plugin
                            .getLogger()
                            .log(
                                Level.SEVERE,
                                "No managed server config found for '${serverInfo.name}'",
                            )
                        cancel("Could not start server")
                        return
                    }

            if (
                !cms.start.allowAnyone && !connection.canStartServer(plugin.proxy, serverInfo.name)
            ) {
                plugin
                    .getLogger()
                    .log(
                        Level.INFO,
                        "Disconnecting '${connection.name}' because they don't have the permission to start server '${serverInfo.name}'",
                    )
                this.cancel("Server is offline and you don't have the permission to start it")
                return
            }

            if (cms.startCmd() == null) {
                plugin
                    .getLogger()
                    .log(
                        Level.SEVERE,
                        "Could not start server '${serverInfo.name}', no start command was set!",
                    )
                this.cancel("Could not start server")
            } else {
                plugin.tryStartServer(cms)
                this.cancel(cms.startKickMsg() ?: "Server is starting")
            }
        }
    }
}
