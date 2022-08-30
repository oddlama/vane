package org.oddlama.velocity.listeners;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.oddlama.vane.proxycore.listeners.LoginEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatServerInfo;
import org.oddlama.velocity.compat.event.VelocityCompatLoginEvent;
import org.oddlama.velocity.compat.event.VelocityCompatPendingConnection;

import java.util.List;
import java.util.Map;

public class ProxyLoginListener {

	Velocity velocity;

	@Inject
	public ProxyLoginListener(Velocity velocity) {
		this.velocity = velocity;
	}

	@Subscribe(order = PostOrder.LAST)
	public void login(com.velocitypowered.api.event.connection.LoginEvent event) {
		if (!event.getResult().isAllowed()) return;

		ProxyServer proxy = velocity.get_raw_proxy();

		final var virtual_host = event.getPlayer().getVirtualHost();
		if (virtual_host.isEmpty()) return;

		Map<String, List<String>> forced_hosts = proxy.getConfiguration().getForcedHosts();

		String forced;
		RegisteredServer server;
		try {
			forced = forced_hosts.get(virtual_host.get().getHostString()).get(0);
			if (forced == null || forced.isEmpty()) throw new Exception();
			server = proxy.getServer(forced).get();
		} catch (Exception ignored) {
			server = proxy.getServer(proxy.getConfiguration().getAttemptConnectionOrder().get(0)).get();
		}

		var server_info = new VelocityCompatServerInfo(server);
		LoginEvent proxy_event = new VelocityCompatLoginEvent(event, velocity, server_info, new VelocityCompatPendingConnection(event.getPlayer()));
		proxy_event.fire();
	}

}
