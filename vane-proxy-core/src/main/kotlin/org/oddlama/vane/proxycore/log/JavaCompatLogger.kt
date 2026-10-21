package org.oddlama.vane.proxycore.log

import java.util.logging.Level
import java.util.logging.Logger

/**
 * [IVaneLogger] implementation backed by `java.util.logging`.
 *
 * @property logger wrapped JUL logger.
 */
class JavaCompatLogger(private val logger: Logger) : IVaneLogger {
    /** Logs [message] at [level]. */
    override fun log(level: Level?, message: String?) = logger.log(level, message)

    /** Logs [message] and [throwable] at [level]. */
    override fun log(level: Level?, message: String?, throwable: Throwable?) =
        logger.log(level, message, throwable)
}
