package org.oddlama.vane.proxycore.scheduler;

public interface ProxyScheduledTask {

	int getId();

	Runnable getTask();

	void cancel();

}
