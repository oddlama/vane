package org.oddlama.vane.proxycore.scheduler

import java.util.concurrent.TimeUnit
import org.oddlama.vane.proxycore.VaneProxyPlugin

/** Scheduler abstraction used by proxy-core for asynchronous and delayed execution. */
interface ProxyTaskScheduler {
    /**
     * Runs [task] asynchronously.
     *
     * @param owner owning plugin context.
     * @param task task to execute.
     * @return scheduled task handle.
     */
    fun runAsync(owner: VaneProxyPlugin?, task: Runnable?): ProxyScheduledTask?

    /**
     * Schedules a one-shot task.
     *
     * @param owner owning plugin context.
     * @param task task to execute.
     * @param delay delay before execution.
     * @param unit unit for [delay].
     * @return scheduled task handle.
     */
    fun schedule(
        owner: VaneProxyPlugin?,
        task: Runnable?,
        delay: Long,
        unit: TimeUnit?,
    ): ProxyScheduledTask?

    /**
     * Schedules a repeating task.
     *
     * @param owner owning plugin context.
     * @param task task to execute.
     * @param delay initial delay.
     * @param period interval between executions.
     * @param unit unit for [delay] and [period].
     * @return scheduled task handle.
     */
    fun schedule(
        owner: VaneProxyPlugin?,
        task: Runnable?,
        delay: Long,
        period: Long,
        unit: TimeUnit?,
    ): ProxyScheduledTask?
}
