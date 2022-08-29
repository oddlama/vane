package org.oddlama.vane.waterfall;

import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;
import org.oddlama.vane.proxycore.ProxyServer;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.log.JavaCompatLogger;
import org.oddlama.vane.waterfall.compat.BungeeCompatProxyServer;
import org.oddlama.vane.waterfall.listeners.ProxyDisconnectListener;
import org.oddlama.vane.waterfall.listeners.ProxyLoginListener;
import org.oddlama.vane.waterfall.listeners.ProxyPingListener;
import org.oddlama.vane.waterfall.listeners.ProxyPreLoginListener;

import java.io.File;

public class Waterfall extends VaneProxyPlugin implements Listener {

	private final WaterfallBasePlugin plugin;
	// bStats
	@SuppressWarnings("unused")
	private Metrics metrics;

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

		plugin_manager.registerListener(plugin, new ProxyPreLoginListener(this));
		plugin_manager.registerListener(plugin, new ProxyLoginListener(this));
		plugin_manager.registerListener(plugin, new ProxyPingListener(this));
		plugin_manager.registerListener(plugin, new ProxyDisconnectListener(this));

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
