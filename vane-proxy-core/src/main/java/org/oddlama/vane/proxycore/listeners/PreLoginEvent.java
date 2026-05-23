package org.oddlama.vane.proxycore.listeners;

import static org.oddlama.vane.proxycore.Util.add_uuid;
import static org.oddlama.vane.proxycore.util.Resolve.resolve_uuid;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.logging.Level;
import org.oddlama.vane.proxycore.Maintenance;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;

public abstract class PreLoginEvent implements ProxyEvent, ProxyCancellableEvent {

    public static String MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK =
        "§cYou have no permission to use this auth multiplexer!";

    public VaneProxyPlugin plugin;

    public PreLoginEvent(VaneProxyPlugin plugin) {
        this.plugin = plugin;
    }

    public void fire() {
        // Not applicable
        assert false;
    }

    public void fire(PreLoginDestination destination) {
        ProxyPendingConnection connection = get_connection();

        // Multiplex authentication if the connection is to a multiplexing port
        final var port = connection.get_port();
        final var multiplexer = plugin.get_config().get_multiplexer_for_port(port);
        if (multiplexer == null) return;

        final var multiplexer_id = multiplexer.getKey();

        // This is pre-authentication, so we need to resolve the uuid ourselves.
        String playerName = connection.get_name();
        UUID uuid;

        try {
            uuid = resolve_uuid(playerName);
        } catch (IOException | URISyntaxException e) {
            plugin.get_logger().log(Level.WARNING, "Failed to resolve UUID for player '" + playerName + "'", e);
            return;
        }

        if (!plugin.can_join_maintenance(uuid)) {
            this.cancel(plugin.get_maintenance().format_message(Maintenance.MESSAGE_CONNECT));
            return;
        }

        if (!multiplexer.getValue().uuid_is_allowed(uuid)) {
            this.cancel(MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK);
            return;
        }

        final var name = connection.get_name();
        final var new_uuid = add_uuid(uuid, multiplexer_id);
        final var new_uuid_str = new_uuid.toString();
        final var new_name = new_uuid_str.substring(new_uuid_str.length() - 16).replace("-", "_");

        plugin
            .get_logger()
            .log(
                Level.INFO,
                "auth multiplex request from player " +
                name +
                " connecting from " +
                connection.get_socket_address().toString()
            );

        MultiplexedPlayer multiplexed_player = new MultiplexedPlayer(multiplexer_id, name, new_name, uuid, new_uuid);
        if (!implementation_specific_auth(multiplexed_player)) {
            return;
        }

        switch (destination) {
            case MULTIPLEXED_UUIDS -> plugin
                .get_multiplexed_uuids()
                .put(multiplexed_player.new_uuid, multiplexed_player.original_uuid);
            case PENDING_MULTIPLEXED_LOGINS -> plugin.get_pending_multiplexer_logins().put(uuid, multiplexed_player);
        }
    }

    public abstract boolean implementation_specific_auth(MultiplexedPlayer multiplexed_player);

    public static void register_auth_multiplex_player(
        IVaneProxyServerInfo server,
        PreLoginEvent.MultiplexedPlayer multiplexed_player
    ) {
        final var stream = new ByteArrayOutputStream();
        final var out = new DataOutputStream(stream);

        try {
            out.writeInt(multiplexed_player.multiplexer_id);
            out.writeUTF(multiplexed_player.original_uuid.toString());
            out.writeUTF(multiplexed_player.name);
            out.writeUTF(multiplexed_player.new_uuid.toString());
            out.writeUTF(multiplexed_player.new_name);
        } catch (IOException e) {
            // This should not happen in a ByteArrayOutputStream, but log it for diagnostics
            java.util.logging.Logger.getLogger(PreLoginEvent.class.getName())
                .log(java.util.logging.Level.SEVERE, "Failed to write multiplexed player data", e);
        }

        server.sendData(stream.toByteArray());
    }

    /** Where to send the details of a PreLoginEvent */
    public enum PreLoginDestination {
        MULTIPLEXED_UUIDS,
        PENDING_MULTIPLEXED_LOGINS,
    }

    public static class MultiplexedPlayer {

        public Integer multiplexer_id;
        public String name;
        public String new_name;
        public UUID original_uuid;
        public UUID new_uuid;

        public MultiplexedPlayer(
            Integer multiplexer_id,
            String name,
            String new_name,
            UUID original_uuid,
            UUID new_uuid
        ) {
            this.multiplexer_id = multiplexer_id;
            this.name = name;
            this.new_name = new_name;
            this.original_uuid = original_uuid;
            this.new_uuid = new_uuid;
        }
    }
}
