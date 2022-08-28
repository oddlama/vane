package org.oddlama.vane.proxycore;

import java.util.concurrent.TimeUnit;

public interface ProxyTaskScheduler {
	ScheduledTask runAsync(VaneProxyPlugin owner, Runnable task);
	ScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, TimeUnit unit);
	ScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, long period, TimeUnit unit);
}
