package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import org.oddlama.vane.proxycore.ProxyPlayer;
import org.oddlama.vane.proxycore.ScheduledTask;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class BungeeCompatProxyServer implements org.oddlama.vane.proxycore.ProxyServer {
	public ProxyServer proxyServer;

	public BungeeCompatProxyServer(ProxyServer proxyServer) {
		this.proxyServer = proxyServer;
	}

	@Override
	public ScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, TimeUnit unit) {
		net.md_5.bungee.api.scheduler.ScheduledTask bungeeTask = proxyServer.getScheduler().schedule((Plugin) owner, task, delay, unit);
		return new BungeeCompatScheduledTask(bungeeTask);
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
