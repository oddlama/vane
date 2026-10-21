package org.oddlama.velocity.compat.scheduler

import com.velocitypowered.api.scheduler.ScheduledTask
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask

/**
 * Proxy-core scheduled task wrapper for Velocity tasks.
 *
 * @property task wrapped Velocity task handle.
 */
class VelocityCompatScheduledTask(val task: ScheduledTask) : ProxyScheduledTask {
    /** Cancels the underlying Velocity task. */
    override fun cancel() = task.cancel()
}
