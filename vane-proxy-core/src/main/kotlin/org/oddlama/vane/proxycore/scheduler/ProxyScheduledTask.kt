package org.oddlama.vane.proxycore.scheduler

/** Handle for a scheduled proxy task. */
interface ProxyScheduledTask {
    /** Cancels future execution of the task. */
    fun cancel()
}
