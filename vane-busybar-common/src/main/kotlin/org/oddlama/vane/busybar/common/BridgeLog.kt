package org.oddlama.vane.busybar.common

import java.util.logging.Level

/** Where the shared BUSY Bar code logs to: a Paper module's logger or the proxy's. */
fun interface BridgeLog {
    /** Logs [message] at [level], with [error] when there is one. */
    fun log(level: Level, message: String, error: Throwable?)

    fun info(message: String) = log(Level.INFO, message, null)

    fun warning(message: String) = log(Level.WARNING, message, null)
}
