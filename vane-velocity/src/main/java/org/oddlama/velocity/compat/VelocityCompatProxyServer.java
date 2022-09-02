package org.oddlama.velocity.compat;

import net.kyori.adventure.text.Component;
import org.oddlama.vane.proxycore.ProxyPlayer;
import org.oddlama.vane.proxycore.ProxyServer;
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler;
import org.oddlama.velocity.compat.scheduler.VelocityCompatProxyTaskScheduler;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

public class VelocityCompatProxyServer implements ProxyServer {

	final com.velocitypowered.api.proxy.ProxyServer proxy;

	public VelocityCompatProxyServer(com.velocitypowered.api.proxy.ProxyServer proxy) {
		this.proxy = proxy;
	}

	@Override
	public ProxyTaskScheduler get_scheduler() {
		return new VelocityCompatProxyTaskScheduler(proxy.getScheduler());
	}

	@Override
	public void broadcast(String message) {
		proxy.sendMessage(Component.text().content(message).asComponent());
	}

	@Override
	public Collection<ProxyPlayer> getPlayers() {
		return proxy.getAllPlayers().stream().map(it -> (ProxyPlayer) new VelocityCompatProxyPlayer(it)).toList();
	}

	@Override
	public boolean has_permission(UUID uuid, String... permission) {
		final var player = proxy.getPlayer(uuid);
		if (player.isEmpty()) return false;

		return Arrays.stream(permission).anyMatch(perm -> player.get().hasPermission(perm));
	}

}
