package org.oddlama.vane.core.lang;

import java.text.MessageFormat;

// Convenience wrapper for parameter auto-packing
public class Message {
	private MessageFormat format;

	public Message(String format) {
		this.format = new MessageFormat(format);
	}

	public String format(Object... args) {
		return format.format(args);
	}
}
