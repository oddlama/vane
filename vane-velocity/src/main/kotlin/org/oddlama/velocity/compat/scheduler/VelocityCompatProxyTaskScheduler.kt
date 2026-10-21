package org.oddlama.velocity.compat.scheduler

import com.velocitypowered.api.scheduler.Scheduler
import java.util.concurrent.TimeUnit
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler

/**
 * Velocity implementation of proxy-core task scheduling.
 *
 * @property scheduler wrapped Velocity scheduler.
 */
class VelocityCompatProxyTaskScheduler(val scheduler: Scheduler) : ProxyTaskScheduler {
    /**
     * Validates nullable scheduling inputs before delegating to a concrete builder.
     *
     * @param owner plugin that owns the task.
     * @param task task runnable.
     * @param unit delay/period time unit.
     * @param build callback building the final scheduled task.
     * @return scheduled task wrapper or `null` when inputs are invalid.
     */
    private inline fun scheduleIfValid(
        owner: VaneProxyPlugin?,
        task: Runnable?,
        unit: TimeUnit?,
        build: (VaneProxyPlugin, Runnable, TimeUnit) -> ProxyScheduledTask,
    ): ProxyScheduledTask? {
        if (owner == null || task == null || unit == null) return null
        return build(owner, task, unit)
    }

    /**
     * Schedules a task to run asynchronously as soon as possible.
     *
     * @param owner plugin that owns the task.
     * @param task task runnable.
     * @return scheduled task wrapper or `null` when inputs are invalid.
     */
    override fun runAsync(owner: VaneProxyPlugin?, task: Runnable?): ProxyScheduledTask? {
        // If owner or task is null, we cannot schedule, so follow a safe null-return policy.
        if (owner == null || task == null) return null

        // https://velocitypowered.com/wiki/developers/task-scheduling/
        // "On Velocity, there is no main thread. All tasks run using the
        // Velocity Scheduler are thus run asynchronously."
        return schedule(owner, task, 0, TimeUnit.SECONDS)
    }

    /**
     * Schedules a one-shot task with delay.
     *
     * @param owner plugin that owns the task.
     * @param task task runnable.
     * @param delay initial delay value.
     * @param unit unit for the delay.
     * @return scheduled task wrapper or `null` when inputs are invalid.
     */
    override fun schedule(
        owner: VaneProxyPlugin?,
        task: Runnable?,
        delay: Long,
        unit: TimeUnit?,
    ): ProxyScheduledTask? {
        return scheduleIfValid(owner, task, unit) { validOwner, validTask, validUnit ->
            VelocityCompatScheduledTask(
                scheduler.buildTask(validOwner, validTask).delay(delay, validUnit).schedule()
            )
        }
    }

    /**
     * Schedules a repeating task with delay and period.
     *
     * @param owner plugin that owns the task.
     * @param task task runnable.
     * @param delay initial delay value.
     * @param period repeat period value.
     * @param unit unit for delay and period.
     * @return scheduled task wrapper or `null` when inputs are invalid.
     */
    override fun schedule(
        owner: VaneProxyPlugin?,
        task: Runnable?,
        delay: Long,
        period: Long,
        unit: TimeUnit?,
    ): ProxyScheduledTask? {
        return scheduleIfValid(owner, task, unit) { validOwner, validTask, validUnit ->
            VelocityCompatScheduledTask(
                scheduler
                    .buildTask(validOwner, validTask)
                    .delay(delay, validUnit)
                    .repeat(period, validUnit)
                    .schedule()
            )
        }
    }
}
