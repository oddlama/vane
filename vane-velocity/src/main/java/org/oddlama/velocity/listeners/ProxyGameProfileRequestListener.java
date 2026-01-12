package org.oddlama.velocity.listeners;

import static org.oddlama.velocity.Util.get_server_for_host;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import java.util.logging.Level;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatServerInfo;

public class ProxyGameProfileRequestListener {

    final Velocity velocity;

    public ProxyGameProfileRequestListener(Velocity velocity) {
        this.velocity = velocity;
    }

    @Subscribe(priority = 0)
    public void game_profile_request(final GameProfileRequestEvent event) {
        // ======= Check we even have a valid pending login =======

        final var virtual_host = event.getConnection().getVirtualHost();
        if (virtual_host.isEmpty()) return;

        final var multiplexer = velocity.get_config().get_multiplexer_for_port(virtual_host.get().getPort());
        if (multiplexer == null) return;

        final var pending_multiplexer_logins = velocity.get_pending_multiplexer_logins();
        if (pending_multiplexer_logins.isEmpty()) return;

        // ====================== End check ======================

        final var profile = event.getGameProfile();
        final var target_uuid = profile.getId();

        PreLoginEvent.MultiplexedPlayer player = pending_multiplexer_logins.remove(target_uuid);
        if (player == null) {
            // We somehow have a multiplexer connection, but it wasn't registered in
            // `pending_multiplexer_logins`
            // Not much to do here; the event isn't cancellable
            velocity.get_logger().log(Level.WARNING, "Unregistered multiplexer connection managed to get through!");
            return;
        }

        final GameProfile tampered_profile = new GameProfile(player.new_uuid, player.new_name, profile.getProperties());
        event.setGameProfile(tampered_profile);

        final var server = get_server_for_host(velocity.get_raw_proxy(), virtual_host.get());
        final var server_info = new VelocityCompatServerInfo(server);
        PreLoginEvent.register_auth_multiplex_player(server_info, player);

        // Now we can finally put our player in `multiplexed_uuids` :)
        velocity.get_multiplexed_uuids().put(player.new_uuid, player.original_uuid);
    }
}
