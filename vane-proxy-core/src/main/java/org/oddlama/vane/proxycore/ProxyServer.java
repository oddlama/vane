package org.oddlama.vane.proxycore;

import java.util.Collection;

public interface ProxyServer {
	ProxyTaskScheduler get_scheduler();

	void broadcast(String message);

	Collection<ProxyPlayer> getPlayers();

	// TODO: getServerInfo()
}
