package org.oddlama.vane.waterfall.listeners;

import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.oddlama.vane.proxycore.listeners.PingEvent;
import org.oddlama.vane.waterfall.Waterfall;
import org.oddlama.vane.waterfall.compat.BungeeCompatServerInfo;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatPingEvent;

public class ProxyPingListener implements Listener {

	Waterfall waterfall;

	public ProxyPingListener(Waterfall plugin) {
		this.waterfall = plugin;
	}

	@EventHandler
	public void on_proxy_ping(ProxyPingEvent event) {
		final PendingConnection connection = event.getConnection();
		var bungeeServerInfo = AbstractReconnectHandler.getForcedHost(connection);
		if (bungeeServerInfo == null) {
			bungeeServerInfo = waterfall.get_plugin().getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
		}

		var server = new BungeeCompatServerInfo(bungeeServerInfo);
		PingEvent proxy_event = new BungeeCompatPingEvent(waterfall, event, server);
		proxy_event.fire();
	}

}
