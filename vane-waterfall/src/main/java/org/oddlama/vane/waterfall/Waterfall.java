package org.oddlama.vane.waterfall;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.UUID;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.bstats.bungeecord.Metrics;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.proxycore.Maintenance;
import org.oddlama.vane.proxycore.ManagedServer;
import org.oddlama.vane.proxycore.ProxyServer;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.ConfigManager;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.listeners.LoginEvent;
import org.oddlama.vane.proxycore.listeners.PingEvent;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.vane.proxycore.log.IVaneLogger;
import org.oddlama.vane.proxycore.log.JavaCompatLogger;
import org.oddlama.vane.waterfall.compat.BungeeCompatProxyServer;
import org.oddlama.vane.waterfall.compat.BungeeCompatServerInfo;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatLoginEvent;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatPendingConnection;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatPingEvent;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatPreLoginEvent;

public class Waterfall extends Plugin implements Listener, VaneProxyPlugin {

	public static final String CHANNEL_AUTH_MULTIPLEX = "vane_waterfall:auth_multiplex";

	public ConfigManager config = new ConfigManager(this);
	public Maintenance maintenance = new Maintenance(this);
	public IVaneLogger logger;
	public BungeeCompatProxyServer server;

	public final LinkedHashMap<UUID, UUID> multiplexedUUIDs = new LinkedHashMap<>();

	// bStats
	@SuppressWarnings("unused")
	private Metrics metrics;

	@Override
	public void onEnable() {
		logger = new JavaCompatLogger(this.getLogger());
		server = new BungeeCompatProxyServer(this.getProxy());

		metrics = new Metrics(this, 8891);

		if (!config.load()) {
			this.onDisable();
			return;
		}

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

		metrics = null;
		logger = null;
	}

	@EventHandler
	public void on_proxy_ping(ProxyPingEvent event) {
		final PendingConnection connection = event.getConnection();
		var bungeeServerInfo = AbstractReconnectHandler.getForcedHost(connection);
		if (bungeeServerInfo == null) {
			bungeeServerInfo = getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
		}

		var server = new BungeeCompatServerInfo(bungeeServerInfo);
		PingEvent proxy_event = new BungeeCompatPingEvent(this, event, server);
		proxy_event.fire();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_pre_login(net.md_5.bungee.api.event.PreLoginEvent event) {
		if (event.isCancelled()) {
			return;
		}

		PreLoginEvent proxy_event = new BungeeCompatPreLoginEvent(this, event);
		proxy_event.fire();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_login(net.md_5.bungee.api.event.LoginEvent event) {
		final var connection = event.getConnection();

		var bungeeServerInfo = AbstractReconnectHandler.getForcedHost(connection);
		if (bungeeServerInfo == null) {
			bungeeServerInfo = getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
		}

		var server = new BungeeCompatServerInfo(bungeeServerInfo);
		LoginEvent proxy_event = new BungeeCompatLoginEvent(event, this, server, new BungeeCompatPendingConnection(connection));
		proxy_event.fire();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_disconnect(ServerDisconnectEvent event) {
		final var uuid = event.getPlayer().getUniqueId();
		multiplexedUUIDs.remove(uuid);
	}

	@Override
	public boolean can_join_maintenance(UUID uuid) {
		if (maintenance.enabled()) {
			// Client is connecting while maintenance is on
			// Players with bypass_maintenance flag may join
			return this.server.has_permission(uuid, "vane_waterfall.bypass_maintenance");
		}

		return true;
	}

	@Override
	public LinkedHashMap<UUID, UUID> get_multiplexed_uuids() {
		return multiplexedUUIDs;
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

	@Override
	public @NotNull Maintenance get_maintenance() {
		return this.maintenance;
	}

	@Override
	public @NotNull ConfigManager get_config() {
		return this.config;
	}

}
