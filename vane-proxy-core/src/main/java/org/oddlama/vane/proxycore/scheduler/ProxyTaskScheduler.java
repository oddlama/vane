package org.oddlama.vane.proxycore.scheduler;

import org.oddlama.vane.proxycore.VaneProxyPlugin;

import java.util.concurrent.TimeUnit;

public interface ProxyTaskScheduler {

	ProxyScheduledTask runAsync(VaneProxyPlugin owner, Runnable task);

	ProxyScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, TimeUnit unit);

	ProxyScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, long period, TimeUnit unit);

}
