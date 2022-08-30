package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import org.oddlama.vane.proxycore.*;
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler;

import java.util.*;

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

	@Override
	public boolean can_start_server(UUID uuid, String serverName) {
		return has_permission(uuid, "vane_waterfall.start_server", "vane_waterfall.start_server.*", "vane_waterfall.start_server." + serverName);
	}

	@Override
	public boolean has_permission(UUID uuid, String... permission) {
		if (uuid == null) {
			return false;
		}

		final var conf_adapter = proxyServer.getConfigurationAdapter();
		for (final var group : conf_adapter.getGroups(uuid.toString())) {
			final var perms = conf_adapter.getList("permissions." + group, null);
			if (perms != null && !Collections.disjoint(perms, List.of(permission))) {
				return true;
			}
		}

		return false;
	}

}
