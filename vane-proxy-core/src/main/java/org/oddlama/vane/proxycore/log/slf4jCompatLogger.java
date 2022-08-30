package org.oddlama.vane.proxycore.log;


import org.slf4j.Logger;

import java.util.logging.Level;

import static java.util.logging.Level.*;

public class slf4jCompatLogger implements IVaneLogger {

	private final Logger logger;

	public slf4jCompatLogger(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void log(Level level, String message) {
		if (INFO.equals(level)) {
			logger.info(message);
		} else if (WARNING.equals(level)) {
			logger.warn(message);
		} else if (SEVERE.equals(level)) {
			logger.error(message);
		}
	}

	@Override
	public void log(Level level, String message, Throwable throwable) {
		if (INFO.equals(level)) {
			logger.info(message, throwable);
		} else if (WARNING.equals(level)) {
			logger.warn(message, throwable);
		} else if (SEVERE.equals(level)) {
			logger.error(message, throwable);
		}
	}

}
