package org.oddlama.vane.waterfall;

import static org.oddlama.vane.proxycore.Util.add_uuid;
import static org.oddlama.vane.util.Resolve.resolve_uuid;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.protocol.packet.LoginRequest;
import org.bstats.bungeecord.Metrics;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.proxycore.Maintenance;
import org.oddlama.vane.proxycore.ManagedServer;
import org.oddlama.vane.proxycore.ProxyServer;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.ConfigManager;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.log.IVaneLogger;
import org.oddlama.vane.proxycore.log.JavaCompatLogger;
import org.oddlama.vane.waterfall.compat.BungeeCompatProxyServer;
import org.oddlama.vane.waterfall.compat.BungeeCompatServerInfo;

public class Waterfall extends Plugin implements Listener, VaneProxyPlugin {

	public static final String CHANNEL_AUTH_MULTIPLEX = "vane_waterfall:auth_multiplex";

	public ConfigManager config = new ConfigManager(this);
	public Maintenance maintenance = new Maintenance(this);
	public IVaneLogger logger;
	public BungeeCompatProxyServer server;

	private final LinkedHashMap<UUID, UUID> multiplexedUUIDs = new LinkedHashMap<>();

	// bStats
	@SuppressWarnings("unused")
	private Metrics metrics;

	@Override
	public void onEnable() {
		logger = new JavaCompatLogger(this.getLogger());
		server = new BungeeCompatProxyServer(this.getProxy());

		metrics = new Metrics(this, 8891);

		config.load();
		maintenance.load();

		final var plugin_manager = getProxy().getPluginManager();
		plugin_manager.registerListener(this, this);
		plugin_manager.registerCommand(this, new org.oddlama.vane.waterfall.commands.Ping());
		plugin_manager.registerCommand(this, new org.oddlama.vane.waterfall.commands.Maintenance(this));

		getProxy().registerChannel(CHANNEL_AUTH_MULTIPLEX);
	}

	@Override
	public void onDisable() {
		final var plugin_manager = getProxy().getPluginManager();
		plugin_manager.unregisterCommands(this);
		plugin_manager.unregisterListeners(this);

		getProxy().unregisterChannel(CHANNEL_AUTH_MULTIPLEX);

		metrics = null;
		logger = null;
	}

	@EventHandler
	public void on_proxy_ping(ProxyPingEvent event) {
		ServerPing server_ping = event.getResponse();

		final PendingConnection connection = event.getConnection();
		var bungeeServerInfo = AbstractReconnectHandler.getForcedHost(connection);
		if (bungeeServerInfo == null) {
			bungeeServerInfo = getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
		}

		var server = new BungeeCompatServerInfo(bungeeServerInfo);
		server_ping.setDescriptionComponent(new TextComponent(TextComponent.fromLegacyText(get_motd(server))));
		server_ping.setFavicon(get_favicon(server.getServerInfo()));

		event.setResponse(server_ping);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_pre_login(PreLoginEvent event) {
		if (event.isCancelled()) {
			return;
		}

		final var connection = event.getConnection();

		// Multiplex authentication if the connection is to a multiplexing port
		final var port = connection.getVirtualHost().getPort();
		final var multiplexer_id = config.multiplexer_by_port.getOrDefault(port, 0);
		if (multiplexer_id > 0) {
			// This is pre-authentication, so we need to resolve the uuid ourselves.
			String playerName = connection.getName();
			UUID uuid;

			try {
				uuid = resolve_uuid(playerName);
			} catch (IOException e) {
				getLogger().log(Level.WARNING, "Failed to resolve UUID for player '" + playerName + "'", e);
				return;
			}

			if (!can_join_maintenance(uuid)) {
				event.setCancelReason(TextComponent.fromLegacyText(maintenance.format_message(Maintenance.MESSAGE_CONNECT)));
				event.setCancelled(true);
				return;
			}

			if (!this.server.has_permission(uuid, "vane_waterfall.auth_multiplexer." + multiplexer_id)) {
				event.setCancelReason(TextComponent.fromLegacyText(MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK));
				event.setCancelled(true);
				return;
			}

			final var name = connection.getName();
			final var new_uuid = add_uuid(uuid, multiplexer_id);
			final var new_uuid_str = new_uuid.toString();
			final var new_name = new_uuid_str.substring(new_uuid_str.length() - 16);

			getLogger()
				.info(
					"auth multiplex request from player " +
					name +
					" connecting from " +
					connection.getSocketAddress().toString()
				);

			try {
				// Change the name of the player
				final var handler_class = Class.forName("net.md_5.bungee.connection.InitialHandler");
				final var request_field = handler_class.getDeclaredField("loginRequest");
				request_field.setAccessible(true);
				final var login_request = (LoginRequest) request_field.get(connection);

				final var data_field = LoginRequest.class.getDeclaredField("data");
				data_field.setAccessible(true);
				data_field.set(login_request, new_name);

				// Set name specifically
				final var name_field = handler_class.getDeclaredField("name");
				name_field.setAccessible(true);
				name_field.set(connection, new_name);
			} catch (
				ClassNotFoundException
				| NoSuchFieldException
				| SecurityException
				| IllegalArgumentException
				| IllegalAccessException e
			) {
				e.printStackTrace();
				return;
			}

			connection.setOnlineMode(false);
			connection.setUniqueId(new_uuid);

			multiplexedUUIDs.put(new_uuid, uuid);
			//final var resulting_uuid = UUID.nameUUIDFromBytes(
			//	("OfflinePlayer:" + new_name).getBytes(StandardCharsets.UTF_8));

			var bungeeServerInfo = AbstractReconnectHandler.getForcedHost(connection);
			if (bungeeServerInfo == null) {
				bungeeServerInfo = getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
			}

			var server = new BungeeCompatServerInfo(bungeeServerInfo);
			register_auth_multiplex_player(server, multiplexer_id, uuid, name, new_uuid, new_name);
			getLogger()
				.info("auth multiplex granted as uuid: " + new_uuid + ", name: " + new_name + " for player " + name);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_login(LoginEvent event) {
		final var connection = event.getConnection();
		final var connection_uuid = connection.getUniqueId();
		final var uuid = multiplexedUUIDs.getOrDefault(connection_uuid, connection_uuid);

		var bungeeServerInfo = AbstractReconnectHandler.getForcedHost(connection);
		if (bungeeServerInfo == null) {
			bungeeServerInfo = getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
		}

		var server = new BungeeCompatServerInfo(bungeeServerInfo);
		if (!can_join_maintenance(uuid)) {
			event.setCancelReason(TextComponent.fromLegacyText(maintenance.format_message(Maintenance.MESSAGE_CONNECT)));
			event.setCancelled(true);
			return;
		}

		// Start server if necessary
		if (!is_online(server)) {
			// For use inside callback
			final var cms = config.managed_servers.get(server.getName());

			if (!this.server.can_start_server(uuid, server.getName())) {
				// TODO: This could probably use a configurable message?
				event.setCancelReason(TextComponent.fromLegacyText("Server is offline"));
				event.setCancelled(true);
				return;
			}

			if (cms == null || cms.start_cmd() == null) {
				getLogger().severe("Could not start server '" + server.getName() + "', no start command was set!");
				event.setCancelReason(TextComponent.fromLegacyText("Could not start server"));
			} else {
				// Client is connecting while startup
				try_start_server(cms);

				if (cms.start_kick_msg() == null) {
					event.setCancelReason(TextComponent.fromLegacyText("Server is starting"));
				} else {
					event.setCancelReason(TextComponent.fromLegacyText(cms.start_kick_msg()));
				}
			}

			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_disconnect(ServerDisconnectEvent event) {
		final var uuid = event.getPlayer().getUniqueId();
		multiplexedUUIDs.remove(uuid);
	}

	public boolean can_join_maintenance(UUID uuid) {
		if (maintenance.enabled()) {
			// Client is connecting while maintenance is on
			// Players with bypass_maintenance flag may join
			return this.server.has_permission(uuid, "vane_waterfall.bypass_maintenance");
		}

		return true;
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

	@Override
	public void register_auth_multiplex_player(IVaneProxyServerInfo server, int multiplexer_id, UUID old_uuid, String old_name, UUID new_uuid, String new_name) {
		final var stream = new ByteArrayOutputStream();
		final var out = new DataOutputStream(stream);

		try {
			out.writeInt(multiplexer_id);
			out.writeUTF(old_uuid.toString());
			out.writeUTF(old_name);
			out.writeUTF(new_uuid.toString());
			out.writeUTF(new_name);
		} catch (IOException e) {
			e.printStackTrace();
		}

		server.sendData(CHANNEL_AUTH_MULTIPLEX, stream.toByteArray());
	}

	@Override
	public boolean is_online(IVaneProxyServerInfo server) {
		final var addr = server.getSocketAddress();
		if (!(addr instanceof final InetSocketAddress inet_addr)) {
			return false;
		}

		var connected = false;
		try (final var test = new Socket(inet_addr.getHostName(), inet_addr.getPort())) {
			connected = test.isConnected();
		} catch (IOException e) {
			// Server not up or not reachable
		}

		return connected;
	}

	@Override
	public String get_motd(IVaneProxyServerInfo server) {
		// Maintenance
		if (maintenance.enabled()) {
			return maintenance.format_message(Maintenance.MOTD);
		}

		final var cms = config.managed_servers.get(server.getName());
		if (cms == null) {
			return "";
		}

		String motd;
		if (is_online(server)) {
			motd = cms.motd_online();
		} else {
			motd = cms.motd_offline();
		}

		return motd;
	}

	@Override
	public void try_start_server(ManagedServer server) {
		// TODO: This could really use some checks (existing process running, nonzero exit code)
		this.server.get_scheduler()
				.runAsync(
						this,
						() -> {
							try {
								final var p = Runtime.getRuntime().exec(server.start_cmd());
								p.waitFor();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
				);
	}

	// TODO: These names are just workarounds for name conflicts for now, temporary
	@Override
	public File getVaneDataFolder() {
		return this.getDataFolder();
	}

	@Override
	public ProxyServer getVaneProxy() {
		return new BungeeCompatProxyServer(this.getProxy());
	}

	@Override
	public @NotNull IVaneLogger getVaneLogger() {
		return logger;
	}

}
