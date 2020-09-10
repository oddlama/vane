package org.oddlama.vane.core.lang;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

public abstract class TranslatedComponent {
	private String key;
	private String default_translation;

	public TranslatedComponent(final String key, final String default_translation) {
		this.key = key;
		this.default_translation = default_translation;
	}

	public String key() { return key; }
	public String default_translation() { return default_translation; }
}
