package org.oddlama.vane.waterfall;

import static org.oddlama.vane.waterfall.Util.add_uuid;

import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.ProxyServer;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import javax.imageio.ImageIO;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.protocol.packet.LoginRequest;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.api.plugin.PluginManager;

public class Waterfall extends Plugin implements Listener {
	public static final String CHANNEL_AUTH_MULTIPLEX = "vane_waterfall:auth_multiplex";
	public static String MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK = "§cYou have no permission to use this auth multiplexer!";

	public Config config = new Config(this);
	public Maintenance maintenance = new Maintenance(this);

	@Override
	public void onEnable() {
		config.load();
		maintenance.load();

		final var plugin_manager = getProxy().getPluginManager();
		plugin_manager.registerListener(this, this);
		plugin_manager.registerCommand(this, new org.oddlama.vane.waterfall.commands.Ping(this));
		plugin_manager.registerCommand(this, new org.oddlama.vane.waterfall.commands.Maintenance(this));

		getProxy().registerChannel(CHANNEL_AUTH_MULTIPLEX);
	}

	@Override
	public void onDisable() {
		final var plugin_manager = getProxy().getPluginManager();
		plugin_manager.unregisterCommands(this);
		plugin_manager.unregisterListeners(this);

		getProxy().unregisterChannel(CHANNEL_AUTH_MULTIPLEX);
	}

	@SuppressWarnings("deprecation")
	@EventHandler
	public void on_proxy_ping(ProxyPingEvent event) {
		ServerPing server_ping = event.getResponse();

		final PendingConnection connection = event.getConnection();
		var server = AbstractReconnectHandler.getForcedHost(connection);
		if (server == null) {
			server = getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
		}

		server_ping.setDescriptionComponent(get_motd(server));
		server_ping.setFavicon(get_favicon(server));

		event.setResponse(server_ping);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_pre_login(PreLoginEvent event) {
		if (event.isCancelled()) {
			return;
		}

		final var connection = event.getConnection();
		var server = AbstractReconnectHandler.getForcedHost(connection);
		if (server == null) {
			server = getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
		}

		if (maintenance.enabled()) {
			// Client is connecting while maintenance is on
			if (has_permission(connection.getUniqueId(), "vane_waterfall.bypass_maintenance")) {
				// Players with bypass_maintenance flag may join
				return;
			}

			event.setCancelReason(maintenance.format_message(Maintenance.MESSAGE_CONNECT));
			event.setCancelled(true);
			return;
		}

		// Start server if necessary
		if (!is_online(server)) {
			// For use inside callback
			final var sinfo = server;
			final var cms = config.managed_servers.get(sinfo.getName());

			if (cms == null || cms.start_cmd() == null) {
				getLogger().severe("Could not start server '" + sinfo.getName() + "', no START_CMD given");
				event.setCancelReason(TextComponent.fromLegacyText("Could not start server"));
			} else {
				// Client is connecting while startup
				getProxy().getScheduler().runAsync(this, () -> {
					try {
						final var p = Runtime.getRuntime().exec(cms.start_cmd());
						p.waitFor();
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

				if (cms.start_kick_msg() == null) {
					event.setCancelReason(TextComponent.fromLegacyText("Server started"));
				} else {
					event.setCancelReason(cms.start_kick_msg());
				}
			}

			event.setCancelled(true);
			return;
		}

		// Multiplex authentication if the connection is to an multiplexing port
		final var port = connection.getVirtualHost().getPort();
		final var multiplex_id = config.multiplexer_by_port.getOrDefault(port, 0);
		if (multiplex_id > 0) {
			final var uuid = connection.getUniqueId();
			if (!has_permission(uuid, "vane_waterfall.multiplex_auth." + multiplex_id)) {
				event.setCancelReason(TextComponent.fromLegacyText(MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK));
				event.setCancelled(true);
				return;
			}

			final var name = uuid.toString().substring(16);
			final var new_uuid = add_uuid(uuid, multiplex_id);
			final var new_name = uuid.toString().substring(16);

			getLogger().info("auth multiplex request from player " + name +
							 " connecting from " + connection.getSocketAddress().toString());

			try {
				// Change the name of the player
				final var handler_class = Class.forName("net.md_5.bungee.connection.InitialHandler");
				final var request_field = handler_class.getDeclaredField("loginRequest");
				request_field.setAccessible(true);
				final var login_request = (LoginRequest)request_field.get(connection);

				final var data_field = LoginRequest.class.getDeclaredField("data");
				data_field.setAccessible(true);
				data_field.set(login_request, new_name);
			} catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				return;
			}

			connection.setOnlineMode(false);
			connection.setUniqueId(new_uuid);

			register_auth_multiplex_player(server, name, new_uuid, new_name);
			getLogger().info("auth multiplex granted as uuid: " + new_uuid + ", name: " + new_name + " for player " + name);
		}
	}

	private void register_auth_multiplex_player(ServerInfo server, String old_name, UUID new_uuid, String new_name) {
		final var stream = new ByteArrayOutputStream();
		final var out = new DataOutputStream(stream);

		try {
			out.writeUTF(old_name);
			out.writeUTF(new_uuid.toString());
			out.writeUTF(new_name);
		} catch (IOException e) {
			e.printStackTrace();
		}

		server.sendData(CHANNEL_AUTH_MULTIPLEX, stream.toByteArray());
	}

	public boolean is_online(final ServerInfo server) {
		final var addr = server.getSocketAddress();
		if (!(addr instanceof InetSocketAddress)) {
			return false;
		}

		final var inet_addr = (InetSocketAddress)addr;
		var connected = false;
		try (final var test = new Socket(inet_addr.getHostName(), inet_addr.getPort())) {
			connected = test.isConnected();
		} catch (IOException e) {
			// Server not up or not reachable
		}

		return connected;
	}

	public BaseComponent get_motd(final ServerInfo server) {
		// Maintenance
		if (maintenance.enabled()) {
			return new TextComponent(maintenance.format_message(Maintenance.MOTD));
		}

		final var cms = config.managed_servers.get(server.getName());
		if (cms == null) {
			return new TextComponent();
		}

		BaseComponent motd = null;
		if (is_online(server)) {
			motd = new TextComponent(cms.motd_offline());
		} else {
			motd = new TextComponent(cms.motd_online());
		}

		return motd == null ? new TextComponent() : motd;
	}

	public Favicon get_favicon(final ServerInfo server) {
		final var cms = config.managed_servers.get(server.getName());
		if (cms == null) {
			return null;
		}

		final var file = cms.favicon_file();
		if (file.exists()) {
			try {
				return Favicon.create(ImageIO.read(file));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public boolean has_permission(UUID uuid, String permission) {
		final var conf_adapter = getProxy().getConfigurationAdapter();
		for (final var group : conf_adapter.getGroups(uuid.toString())) {
			final var perms = conf_adapter.getList("permissions." + group, null);
			if (perms != null && (perms.contains("vane_waterfall." + permission) || perms.contains("vane_waterfall.*") || perms.contains("*"))) {
				return true;
			}
		}

		return false;
	}
}
