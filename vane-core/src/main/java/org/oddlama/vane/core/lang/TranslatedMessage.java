package org.oddlama.vane.core.lang;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

public class TranslatedMessage {
	private String key;
	private String default_translation;

	public TranslatedMessage(String format) {
		this.format = new MessageFormat(format);
	}

	public TranslatedMessage(final String key, final String default_translation) {
		this.key = key;
		this.default_translation = default_translation;
	}

	public TranslatedMessage(final TranslatedMessage copy_of) {
		this.key = copy_of.key;
		this.default_translation = copy_of.default_translation;
	}

	public TranslatableComponent format(Object... args) {
		return new TranslatableComponent(key, args);
	}

	public String str_format(Object... args) {
		return default_translation.format(args);
	}

	public String key() { return key; }
	public String str() { return default_translation; }
}
