package org.oddlama.vane.waterfall.listeners;


import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.oddlama.vane.proxycore.listeners.LoginEvent;
import org.oddlama.vane.waterfall.Waterfall;
import org.oddlama.vane.waterfall.compat.BungeeCompatServerInfo;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatLoginEvent;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatPendingConnection;

public class ProxyLoginListener implements Listener {

	Waterfall waterfall;

	public ProxyLoginListener(Waterfall waterfall) {
		this.waterfall = waterfall;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_login(net.md_5.bungee.api.event.LoginEvent event) {
		final var connection = event.getConnection();

		var bungeeServerInfo = AbstractReconnectHandler.getForcedHost(connection);
		if (bungeeServerInfo == null) {
			bungeeServerInfo = waterfall.get_plugin().getProxy().getServerInfo(connection.getListener().getServerPriority().get(0));
		}

		var server = new BungeeCompatServerInfo(bungeeServerInfo);
		LoginEvent proxy_event = new BungeeCompatLoginEvent(event, waterfall, server, new BungeeCompatPendingConnection(connection));
		proxy_event.fire();
	}

}
