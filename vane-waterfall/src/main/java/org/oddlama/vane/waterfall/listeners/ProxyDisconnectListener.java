package org.oddlama.vane.waterfall.listeners;

import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.oddlama.vane.waterfall.Waterfall;

public class ProxyDisconnectListener implements Listener {

	Waterfall waterfall;

	public ProxyDisconnectListener(Waterfall waterfall) {
		this.waterfall = waterfall;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void on_disconnect(ServerDisconnectEvent event) {
		final var uuid = event.getPlayer().getUniqueId();
		waterfall.get_multiplexed_uuids().remove(uuid);
	}

}
