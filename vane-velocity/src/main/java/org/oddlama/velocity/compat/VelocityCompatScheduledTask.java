package org.oddlama.velocity.compat;

import com.velocitypowered.api.scheduler.ScheduledTask;
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask;

public class VelocityCompatScheduledTask implements ProxyScheduledTask {

	ScheduledTask task;

	public VelocityCompatScheduledTask(ScheduledTask task) {
		this.task = task;
	}

	@Override
	public void cancel() {
		task.cancel();
	}

}
