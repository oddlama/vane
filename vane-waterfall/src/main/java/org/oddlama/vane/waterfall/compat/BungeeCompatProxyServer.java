package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import org.oddlama.vane.proxycore.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeeCompatProxyServer implements org.oddlama.vane.proxycore.ProxyServer {
	public ProxyServer proxyServer;

	public BungeeCompatProxyServer(ProxyServer proxyServer) {
		this.proxyServer = proxyServer;
	}

	@Override
	public ProxyTaskScheduler get_scheduler() {
		return new BungeeCompatProxyTaskScheduler(proxyServer.getScheduler());
	}

	@Override
	public void broadcast(String message) {
		proxyServer.broadcast(TextComponent.fromLegacyText(message));
	}

	@Override
	public Collection<ProxyPlayer> getPlayers() {
		return proxyServer.getPlayers().stream().map(it -> (ProxyPlayer) new BungeeCompatProxyPlayer(it)).toList();
	}

}
