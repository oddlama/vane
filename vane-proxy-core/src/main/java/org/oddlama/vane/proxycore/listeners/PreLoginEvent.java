package org.oddlama.vane.proxycore.listeners;

import org.oddlama.vane.proxycore.Maintenance;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

import static org.oddlama.vane.proxycore.Util.add_uuid;
import static org.oddlama.vane.util.Resolve.resolve_uuid;

public abstract class PreLoginEvent implements ProxyEvent, ProxyCancellableEvent {

	public static String MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK =
			"§cYou have no permission to use this auth multiplexer!";

	public VaneProxyPlugin plugin;

	public PreLoginEvent(VaneProxyPlugin plugin) {
		this.plugin = plugin;
	}

	public void fire() {
		ProxyPendingConnection connection = get_connection();

		// Multiplex authentication if the connection is to a multiplexing port
		final var port = connection.get_port();
		final var multiplexer_id = plugin.get_config().multiplexer_by_port.getOrDefault(port, 0);
		if (multiplexer_id > 0) {
			// This is pre-authentication, so we need to resolve the uuid ourselves.
			String playerName = connection.get_name();
			UUID uuid;

			try {
				uuid = resolve_uuid(playerName);
			} catch (IOException e) {
				plugin.get_logger().log(Level.WARNING, "Failed to resolve UUID for player '" + playerName + "'", e);
				return;
			}

			if (!plugin.can_join_maintenance(uuid)) {
				this.cancel(plugin.get_maintenance().format_message(Maintenance.MESSAGE_CONNECT));
				return;
			}

			if (!plugin.get_proxy().has_permission(uuid, "vane_waterfall.auth_multiplexer." + multiplexer_id)) {
				this.cancel(MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK);
				return;
			}

			final var name = connection.get_name();
			final var new_uuid = add_uuid(uuid, multiplexer_id);
			final var new_uuid_str = new_uuid.toString();
			final var new_name = new_uuid_str.substring(new_uuid_str.length() - 16);

			plugin.get_logger()
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

			plugin.get_multiplexed_uuids().put(multiplexed_player.new_uuid, multiplexed_player.original_uuid);
		}
	}

	public abstract boolean implementation_specific_auth(MultiplexedPlayer multiplexed_player);

	public abstract void register_auth_multiplex_player(IVaneProxyServerInfo server, PreLoginEvent.MultiplexedPlayer multiplexed_player);

	public static class MultiplexedPlayer {

		public Integer multiplexer_id;
		public String name;
		public String new_name;
		public UUID original_uuid;
		public UUID new_uuid;

		public MultiplexedPlayer(Integer multiplexer_id, String name, String new_name, UUID original_uuid, UUID new_uuid) {
			this.multiplexer_id = multiplexer_id;
			this.name = name;
			this.new_name = new_name;
			this.original_uuid = original_uuid;
			this.new_uuid = new_uuid;
		}

	}

}
