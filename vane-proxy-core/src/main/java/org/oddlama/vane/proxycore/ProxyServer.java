package org.oddlama.vane.proxycore;

import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler;

import java.util.Collection;
import java.util.UUID;

public interface ProxyServer {

	ProxyTaskScheduler get_scheduler();

	void broadcast(String message);

	Collection<ProxyPlayer> getPlayers();

	boolean has_permission(UUID uuid, String... permission);

}
