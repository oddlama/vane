package org.oddlama.vane.proxycore;

import java.util.Collection;

public interface ProxyServer {
	ScheduledTask schedule(VaneProxyPlugin owner,
						   Runnable task,
						   long delay,
						   java.util.concurrent.TimeUnit unit);

	void broadcast(String message);

	Collection<ProxyPlayer> getPlayers();
}
