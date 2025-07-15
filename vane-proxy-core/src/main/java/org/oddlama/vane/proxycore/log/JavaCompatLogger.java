package org.oddlama.vane.proxycore.log;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaCompatLogger implements IVaneLogger {

    private final Logger logger;

    public JavaCompatLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void log(Level level, String message) {
        logger.log(level, message);
    }

    @Override
    public void log(Level level, String message, Throwable throwable) {
        logger.log(level, message, throwable);
    }
}
