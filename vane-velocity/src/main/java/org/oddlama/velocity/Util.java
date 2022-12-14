package org.oddlama.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

public class Util {

	public static RegisteredServer get_server_for_host(ProxyServer proxy, InetSocketAddress host) {
		Map<String, List<String>> forced_hosts = proxy.getConfiguration().getForcedHosts();

		String forced;
		RegisteredServer server;
		try {
			forced = forced_hosts.get(host.getHostString()).get(0);
			if (forced == null || forced.isEmpty()) throw new Exception();
			server = proxy.getServer(forced).get();
		} catch (Exception ignored) {
			server = proxy.getServer(proxy.getConfiguration().getAttemptConnectionOrder().get(0)).get();
		}

		return server;
	}

}
