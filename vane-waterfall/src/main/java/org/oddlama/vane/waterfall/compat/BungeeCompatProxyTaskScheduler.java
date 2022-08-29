package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.scheduler.TaskScheduler;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask;
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler;
import org.oddlama.vane.waterfall.Waterfall;

import java.util.concurrent.TimeUnit;

public class BungeeCompatProxyTaskScheduler implements ProxyTaskScheduler {

	TaskScheduler scheduler;

	public BungeeCompatProxyTaskScheduler(TaskScheduler scheduler) {
		this.scheduler = scheduler;
	}


	@Override
	public ProxyScheduledTask runAsync(VaneProxyPlugin owner, Runnable task) {
		net.md_5.bungee.api.scheduler.ScheduledTask bungeeTask = scheduler.runAsync(((Waterfall) owner).get_plugin(), task);
		return new BungeeCompatProxyScheduledTask(bungeeTask);
	}

	@Override
	public ProxyScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, TimeUnit unit) {
		net.md_5.bungee.api.scheduler.ScheduledTask bungeeTask = scheduler.schedule(((Waterfall) owner).get_plugin(), task, delay, unit);
		return new BungeeCompatProxyScheduledTask(bungeeTask);
	}

	@Override
	public ProxyScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, long period, TimeUnit unit) {
		net.md_5.bungee.api.scheduler.ScheduledTask bungeeTask = scheduler.schedule(((Waterfall) owner).get_plugin(), task, delay, period, unit);
		return new BungeeCompatProxyScheduledTask(bungeeTask);
	}

}
