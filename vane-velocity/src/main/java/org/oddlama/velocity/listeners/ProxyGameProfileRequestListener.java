package org.oddlama.velocity.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.GameProfile;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatServerInfo;

import java.util.logging.Level;

import static org.oddlama.velocity.Util.get_server_for_host;

public class ProxyGameProfileRequestListener {

	Velocity velocity;

	public ProxyGameProfileRequestListener(Velocity velocity) {
		this.velocity = velocity;
	}

	@Subscribe(order = PostOrder.LAST)
	public void pre_login(final GameProfileRequestEvent event) {
		final var virtual_host = event.getConnection().getVirtualHost();
		if (virtual_host.isEmpty()) return;

		Integer multiplexer_id = velocity.get_config().multiplexer_by_port.get(virtual_host.get().getPort());
		if (multiplexer_id == null) return;

		if (velocity.pending_multiplexer_logins.isEmpty()) return;

		final var profile = event.getGameProfile();
		final var target_uuid = profile.getId();

		PreLoginEvent.MultiplexedPlayer player = velocity.pending_multiplexer_logins.remove(target_uuid);
		if (player == null) {
			velocity.get_logger().log(Level.WARNING, "Unregistered multiplexer connection managed to get through!");
			return;
		}

		final GameProfile tampered_profile = new GameProfile(player.new_uuid, player.new_name, profile.getProperties());
		event.setGameProfile(tampered_profile);

		final var server = get_server_for_host(velocity.get_raw_proxy(), virtual_host.get());
		final var server_info = new VelocityCompatServerInfo(server);
		PreLoginEvent.register_auth_multiplex_player(server_info, player);

		velocity.get_multiplexed_uuids().put(player.new_uuid, player.original_uuid);
	}

}
