package org.oddlama.vane.core.persistent;

@SuppressWarnings("serial")
public class LoadException extends Exception {
	public LoadException(String message) {
		super(message);
	}

	public LoadException(String message, Exception cause) {
		super(message, cause);
	}
}
