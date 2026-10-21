package org.oddlama.vane.proxycore.log

import java.util.logging.Level
import org.slf4j.Logger

/**
 * [IVaneLogger] implementation backed by SLF4J.
 *
 * @property logger wrapped SLF4J logger.
 */
class Slf4jCompatLogger(private val logger: Logger) : IVaneLogger {
    /** Logs [message] at [level], mapping known JUL levels to SLF4J APIs. */
    override fun log(level: Level?, message: String?) {
        when (level) {
            Level.INFO -> logger.info(message)
            Level.WARNING -> logger.warn(message)
            Level.SEVERE -> logger.error(message)
            else -> Unit
        }
    }

    /** Logs [message] and [throwable] at [level], mapping known JUL levels to SLF4J APIs. */
    override fun log(level: Level?, message: String?, throwable: Throwable?) {
        when (level) {
            Level.INFO -> logger.info(message, throwable)
            Level.WARNING -> logger.warn(message, throwable)
            Level.SEVERE -> logger.error(message, throwable)
            else -> Unit
        }
    }
}
