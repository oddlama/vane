package org.oddlama.vane.proxycore.log

import java.util.logging.Level

/** Minimal logger abstraction for proxy-core across logging backends. */
interface IVaneLogger {
    /**
     * Logs a message at [level].
     *
     * @param level log severity.
     * @param message message text.
     */
    fun log(level: Level?, message: String?)

    /**
     * Logs a message and associated [throwable] at [level].
     *
     * @param level log severity.
     * @param message message text.
     * @param throwable exception to log.
     */
    fun log(level: Level?, message: String?, throwable: Throwable?)
}
