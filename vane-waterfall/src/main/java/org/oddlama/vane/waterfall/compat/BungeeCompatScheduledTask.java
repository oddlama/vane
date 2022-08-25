package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

public class BungeeCompatScheduledTask implements org.oddlama.vane.proxycore.ScheduledTask {
	public ScheduledTask task;

	public BungeeCompatScheduledTask(ScheduledTask task) {
		this.task = task;
	}

	@Override
	public int getId() {
		return task.getId();
	}

	@Override
	public VaneProxyPlugin getOwner() {
		return (VaneProxyPlugin) task.getOwner();
	}

	@Override
	public Runnable getTask() {
		return task.getTask();
	}

	@Override
	public void cancel() {
		task.cancel();
	}

}
