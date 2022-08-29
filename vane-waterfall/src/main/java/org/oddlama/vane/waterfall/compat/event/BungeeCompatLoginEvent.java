package org.oddlama.vane.waterfall.compat.event;

import net.md_5.bungee.api.chat.TextComponent;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.listeners.LoginEvent;

public class BungeeCompatLoginEvent extends LoginEvent {

	net.md_5.bungee.api.event.LoginEvent event;

	public BungeeCompatLoginEvent(net.md_5.bungee.api.event.LoginEvent event, VaneProxyPlugin plugin, IVaneProxyServerInfo server_info, ProxyPendingConnection connection) {
		super(plugin, server_info, connection);
		this.event = event;
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
		return null;
	}

}
