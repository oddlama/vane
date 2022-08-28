package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import org.oddlama.vane.proxycore.ProxyTaskScheduler;
import org.oddlama.vane.proxycore.ScheduledTask;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

import java.util.concurrent.TimeUnit;

public class BungeeCompatProxyTaskScheduler implements ProxyTaskScheduler {
	TaskScheduler scheduler;

	public BungeeCompatProxyTaskScheduler(TaskScheduler scheduler) {
		this.scheduler = scheduler;
	}


	@Override
	public ScheduledTask runAsync(VaneProxyPlugin owner, Runnable task) {
		net.md_5.bungee.api.scheduler.ScheduledTask bungeeTask = scheduler.runAsync((Plugin) owner, task);
		return new BungeeCompatScheduledTask(bungeeTask);
	}

	@Override
	public ScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, TimeUnit unit) {
		net.md_5.bungee.api.scheduler.ScheduledTask bungeeTask = scheduler.schedule((Plugin) owner, task, delay, unit);
		return new BungeeCompatScheduledTask(bungeeTask);
	}

	@Override
	public ScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, long period, TimeUnit unit) {
		net.md_5.bungee.api.scheduler.ScheduledTask bungeeTask = scheduler.schedule((Plugin) owner, task, delay, period, unit);
		return new BungeeCompatScheduledTask(bungeeTask);
	}

}
