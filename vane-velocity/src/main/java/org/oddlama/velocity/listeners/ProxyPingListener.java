package org.oddlama.velocity.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import org.oddlama.vane.proxycore.listeners.PingEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatServerInfo;
import org.oddlama.velocity.compat.event.VelocityCompatPingEvent;

import static org.oddlama.velocity.Util.get_server_for_host;

public class ProxyPingListener {

	final Velocity velocity;

	public ProxyPingListener(Velocity velocity) {
		this.velocity = velocity;
	}

	@Subscribe
	public void on_proxy_ping(ProxyPingEvent event) {
		ProxyServer proxy = velocity.get_raw_proxy();

		final var virtual_host = event.getConnection().getVirtualHost();
		if (virtual_host.isEmpty()) return;

		final var server = get_server_for_host(proxy, virtual_host.get());

		var server_info = new VelocityCompatServerInfo(server);
		PingEvent proxy_event = new VelocityCompatPingEvent(velocity, event, server_info);
		proxy_event.fire();
	}

}

