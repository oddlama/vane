package org.oddlama.vane.waterfall.compat.event;

import net.md_5.bungee.api.event.ProxyPingEvent;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.listeners.PingEvent;
import org.oddlama.vane.waterfall.compat.BungeeCompatProxyServerPing;

public class BungeeCompatPingEvent extends PingEvent {

	ProxyPingEvent event;

	public BungeeCompatPingEvent(VaneProxyPlugin plugin, ProxyPingEvent event, IVaneProxyServerInfo server) {
		super(plugin, new BungeeCompatProxyServerPing(event.getResponse()), server);
		this.event = event;
	}

	@Override
	public ProxyPendingConnection get_connection() {
		return null;
	}

	@Override
	public void send_response() {
		event.setResponse(event.getResponse());
	}

}
