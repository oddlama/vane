package org.oddlama.vane.waterfall.compat.event;

import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.protocol.packet.LoginRequest;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.vane.waterfall.Waterfall;
import org.oddlama.vane.waterfall.compat.BungeeCompatServerInfo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;

public class BungeeCompatPreLoginEvent extends PreLoginEvent {

	net.md_5.bungee.api.event.PreLoginEvent event;
	Plugin bungee_plugin;

	public BungeeCompatPreLoginEvent(Waterfall waterfall, net.md_5.bungee.api.event.PreLoginEvent event) {
		super(waterfall);
		this.event = event;
		this.bungee_plugin = waterfall.get_plugin();
	}

	@Override
	public void cancel() {
		event.setCancelled(true);
	}

	@Override
	public void cancel(String reason) {
		event.setCancelReason(TextComponent.fromLegacyText(reason));
		event.setCancelled(true);
	}

	@Override
	public ProxyPendingConnection get_connection() {
		return new BungeeCompatPendingConnection(event.getConnection());
	}

	@Override
	public boolean implementation_specific_auth(MultiplexedPlayer multiplexed_player) {
		final var connection = event.getConnection();

		try {
			// Change the name of the player
			final var handler_class = Class.forName("net.md_5.bungee.connection.InitialHandler");
			final var request_field = handler_class.getDeclaredField("loginRequest");
			request_field.setAccessible(true);
			final var login_request = (LoginRequest) request_field.get(connection);

			final var data_field = LoginRequest.class.getDeclaredField("data");
			data_field.setAccessible(true);
			data_field.set(login_request, multiplexed_player.new_name);

			// Set name specifically
			final var name_field = handler_class.getDeclaredField("name");
			name_field.setAccessible(true);
			name_field.set(connection, multiplexed_player.new_name);
		} catch (
				ClassNotFoundException
				| NoSuchFieldException
				| SecurityException
				| IllegalArgumentException
				| IllegalAccessException e
		) {
			e.printStackTrace();
			return false;
		}

		final var name = multiplexed_player.name;
		final var new_name = multiplexed_player.new_name;
		final var new_uuid = multiplexed_player.new_uuid;

		connection.setOnlineMode(false);
		connection.setUniqueId(multiplexed_player.new_uuid);

		var bungeeServerInfo = AbstractReconnectHandler.getForcedHost(connection);
		if (bungeeServerInfo == null) {
			bungeeServerInfo = bungee_plugin.getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
		}

		var server = new BungeeCompatServerInfo(bungeeServerInfo);
		register_auth_multiplex_player(server, multiplexed_player);
		plugin.get_logger()
				.log(Level.INFO, "auth multiplex granted as uuid: " + new_uuid + ", name: " + new_name + " for player " + name);

		return true;
	}

	@Override
	public void register_auth_multiplex_player(IVaneProxyServerInfo server, MultiplexedPlayer multiplexed_player) {
		final var stream = new ByteArrayOutputStream();
		final var out = new DataOutputStream(stream);

		try {
			out.writeInt(multiplexed_player.multiplexer_id);
			out.writeUTF(multiplexed_player.original_uuid.toString());
			out.writeUTF(multiplexed_player.name);
			out.writeUTF(multiplexed_player.new_uuid.toString());
			out.writeUTF(multiplexed_player.new_name);
		} catch (IOException e) {
			e.printStackTrace();
		}

		server.sendData(Waterfall.CHANNEL_AUTH_MULTIPLEX, stream.toByteArray());
	}

}
