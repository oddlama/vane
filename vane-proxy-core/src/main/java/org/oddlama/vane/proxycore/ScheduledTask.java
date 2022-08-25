package org.oddlama.vane.proxycore;

public interface ScheduledTask {
	int getId();

	VaneProxyPlugin getOwner();

	Runnable getTask();

	void cancel();
}
