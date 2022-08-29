package org.oddlama.vane.waterfall.listeners;

import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.vane.waterfall.Waterfall;
import org.oddlama.vane.waterfall.compat.event.BungeeCompatPreLoginEvent;

public class ProxyPreLoginListener implements Listener {

	Waterfall waterfall;

	public ProxyPreLoginListener(Waterfall waterfall) {
		this.waterfall = waterfall;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_pre_login(net.md_5.bungee.api.event.PreLoginEvent event) {
		if (event.isCancelled()) {
			return;
		}

		PreLoginEvent proxy_event = new BungeeCompatPreLoginEvent(waterfall, event);
		proxy_event.fire();
	}

}
