package org.oddlama.vane.proxycore.listeners;

import java.util.logging.Level;
import org.oddlama.vane.proxycore.Maintenance;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;

public abstract class LoginEvent implements ProxyEvent, ProxyCancellableEvent {

    VaneProxyPlugin plugin;
    IVaneProxyServerInfo server_info;
    ProxyPendingConnection connection;

    public LoginEvent(VaneProxyPlugin plugin, IVaneProxyServerInfo server_info, ProxyPendingConnection connection) {
        this.plugin = plugin;
        this.server_info = server_info;
        this.connection = connection;
    }

    public final void fire() {
        final var connection_uuid = connection.get_unique_id();

        // We're in the LoginEvent, the UUID should be resolved
        assert connection_uuid != null;

        final var uuid = plugin.get_multiplexed_uuids().getOrDefault(connection_uuid, connection_uuid);

        if (!plugin.can_join_maintenance(uuid)) {
            this.cancel(plugin.get_maintenance().format_message(Maintenance.MESSAGE_CONNECT));
            return;
        }

        plugin
            .get_logger()
            .log(
                Level.INFO,
                "Connection '" + connection.get_name() + "' is connecting to '" + server_info.getName() + "'"
            );

        // Start server if necessary
        if (!plugin.is_online(server_info)) {
            // For use inside callback
            final var cms = plugin.get_config().managed_servers.get(server_info.getName());

            if (!cms.start.allow_anyone && !connection.can_start_server(plugin.get_proxy(), server_info.getName())) {
                plugin
                    .get_logger()
                    .log(
                        Level.INFO,
                        "Disconnecting '" +
                        connection.get_name() +
                        "' because they don't have the permission to start server '" +
                        server_info.getName() +
                        "'"
                    );
                // TODO: This could probably use a configurable message?
                this.cancel("Server is offline and you don't have the permission to start it");
                return;
            }

            if (cms == null || cms.start_cmd() == null) {
                plugin
                    .get_logger()
                    .log(
                        Level.SEVERE,
                        "Could not start server '" + server_info.getName() + "', no start command was set!"
                    );
                this.cancel("Could not start server");
            } else {
                // Client is connecting while startup
                plugin.try_start_server(cms);

                if (cms.start_kick_msg() == null) {
                    this.cancel("Server is starting");
                } else {
                    this.cancel(cms.start_kick_msg());
                }
            }
        }
    }
}
