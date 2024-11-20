package org.oddlama.vane.proxycore.scheduler;

import java.util.concurrent.TimeUnit;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

public interface ProxyTaskScheduler {
    ProxyScheduledTask runAsync(VaneProxyPlugin owner, Runnable task);

    ProxyScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, TimeUnit unit);

    ProxyScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, long period, TimeUnit unit);
}
