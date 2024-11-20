package org.oddlama.velocity.compat.scheduler;

import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import java.util.concurrent.TimeUnit;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask;
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler;

public class VelocityCompatProxyTaskScheduler implements ProxyTaskScheduler {

    final Scheduler scheduler;

    public VelocityCompatProxyTaskScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public ProxyScheduledTask runAsync(VaneProxyPlugin owner, Runnable task) {
        // https://velocitypowered.com/wiki/developers/task-scheduling/
        //
        // "On Velocity, there is no main thread. All tasks run using the
        // Velocity Scheduler are thus run asynchronously."
        return schedule(owner, task, 0, TimeUnit.SECONDS);
    }

    @Override
    public ProxyScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, TimeUnit unit) {
        ScheduledTask velocity_task = scheduler.buildTask(owner, task).delay(delay, unit).schedule();
        return new VelocityCompatScheduledTask(velocity_task);
    }

    @Override
    public ProxyScheduledTask schedule(VaneProxyPlugin owner, Runnable task, long delay, long period, TimeUnit unit) {
        ScheduledTask velocity_task = scheduler
            .buildTask(owner, task)
            .delay(delay, unit)
            .repeat(period, unit)
            .schedule();
        return new VelocityCompatScheduledTask(velocity_task);
    }
}
