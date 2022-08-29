package org.oddlama.vane.waterfall;

import java.io.File;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.bstats.bungeecord.Metrics;
import org.oddlama.vane.proxycore.ProxyServer;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.listeners.LoginEvent;
import org.oddlama.vane.proxycore.listeners.PingEvent;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.vane.proxycore.log.JavaCompatLogger;
import org.oddlama.vane.waterfall.compat.BungeeCompatProxyServer;
import org.oddlama.vane.waterfall.compat.BungeeCompatServerInfo;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatLoginEvent;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatPendingConnection;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatPingEvent;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatPreLoginEvent;

public class Waterfall extends VaneProxyPlugin implements Listener {
	// bStats
	@SuppressWarnings("unused")
	private Metrics metrics;
	private final WaterfallBasePlugin plugin;

	public Waterfall(WaterfallBasePlugin plugin) {
		this.plugin = plugin;
	}

	public void enable() {
		logger = new JavaCompatLogger(plugin.getLogger());
		server = new BungeeCompatProxyServer(plugin.getProxy());

		metrics = new Metrics(plugin, 8891);

		if (!config.load()) {
			plugin.onDisable();
			return;
		}

		maintenance.load();

		final var proxy = plugin.getProxy();
		final var plugin_manager = proxy.getPluginManager();
		plugin_manager.registerListener(plugin, this);
		plugin_manager.registerCommand(plugin, new org.oddlama.vane.waterfall.commands.Ping(this));
		plugin_manager.registerCommand(plugin, new org.oddlama.vane.waterfall.commands.Maintenance(this));

		proxy.registerChannel(CHANNEL_AUTH_MULTIPLEX);
	}

	public void disable() {
		final var proxy = plugin.getProxy();
		final var plugin_manager = proxy.getPluginManager();
		plugin_manager.unregisterCommands(plugin);
		plugin_manager.unregisterListeners(plugin);

		proxy.unregisterChannel(CHANNEL_AUTH_MULTIPLEX);

		metrics = null;
		logger = null;
	}

	@EventHandler
	public void on_proxy_ping(ProxyPingEvent event) {
		final PendingConnection connection = event.getConnection();
		var bungeeServerInfo = AbstractReconnectHandler.getForcedHost(connection);
		if (bungeeServerInfo == null) {
			bungeeServerInfo = plugin.getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
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
			bungeeServerInfo = plugin.getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
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
	public File get_data_folder() {
		return plugin.getDataFolder();
	}

	@Override
	public ProxyServer get_proxy() {
		return new BungeeCompatProxyServer(plugin.getProxy());
	}

	public Plugin get_plugin() {
		return plugin;
	}

}
