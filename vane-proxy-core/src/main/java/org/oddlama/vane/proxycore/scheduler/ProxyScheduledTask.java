package org.oddlama.vane.proxycore.scheduler;

import org.oddlama.vane.proxycore.VaneProxyPlugin;

public interface ProxyScheduledTask {
	int getId();

	VaneProxyPlugin getOwner();

	Runnable getTask();

	void cancel();
}
