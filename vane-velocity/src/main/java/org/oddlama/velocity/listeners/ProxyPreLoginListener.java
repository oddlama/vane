package org.oddlama.velocity.listeners;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.event.VelocityCompatPreLoginEvent;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

import static org.oddlama.vane.proxycore.Util.add_uuid;
import static org.oddlama.vane.util.Resolve.resolve_uuid;

public class ProxyPreLoginListener {

	Velocity velocity;

	@Inject
	public ProxyPreLoginListener(Velocity velocity) {
		this.velocity = velocity;
	}

	@Subscribe
	public void pre_login(final com.velocitypowered.api.event.connection.PreLoginEvent event) {
		PreLoginEvent proxy_event = new VelocityCompatPreLoginEvent(velocity, event);

		final var virtual_host = event.getConnection().getVirtualHost();
		if (virtual_host.isPresent()) {
			Integer multiplexer_id = velocity.get_config().multiplexer_by_port.get(virtual_host.get().getPort());
			if (multiplexer_id != null) {
				String player_name = event.getUsername();
				UUID uuid;
				try {
					uuid = resolve_uuid(player_name);
				} catch (IOException e) {
					String msg = "Failed to resolve UUID for player '" + player_name + "'";
					velocity.get_logger().log(Level.WARNING, msg, e);
					proxy_event.cancel(msg);
					return;
				}

				final var new_uuid = add_uuid(uuid, multiplexer_id);
				final var new_uuid_str = new_uuid.toString();
				final var new_name = new_uuid_str.substring(new_uuid_str.length() - 16);

				final var multiplexed_player = new PreLoginEvent.MultiplexedPlayer(multiplexer_id, player_name, new_name, uuid, new_uuid);

				velocity.get_logger()
						.log(Level.INFO,
								"auth multiplex granted as uuid: "
										+ multiplexed_player.new_uuid
										+ ", name: "
										+ multiplexed_player.new_name
										+ " for player "
										+ multiplexed_player.name
						);

				velocity.pending_multiplexer_logins.put(uuid, multiplexed_player);
				return;
			}
		}

		proxy_event.fire();
	}

}
