package org.oddlama.vane.proxycore.log;

import java.util.logging.Level;

public interface IVaneLogger {
    void log(Level level, String message);

    void log(Level level, String message, Throwable throwable);
}
