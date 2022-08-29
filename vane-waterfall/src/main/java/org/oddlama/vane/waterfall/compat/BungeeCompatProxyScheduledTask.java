package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask;

public class BungeeCompatProxyScheduledTask implements ProxyScheduledTask {

	public ScheduledTask task;

	public BungeeCompatProxyScheduledTask(ScheduledTask task) {
		this.task = task;
	}

	@Override
	public int getId() {
		return task.getId();
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
