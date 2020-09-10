package org.oddlama.vane.core.lang;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

public class TranslatedString extends TranslatedComponent {
	public TranslatedString(final String key, final String default_translation) {
		super(key, default_translation);
	}

	public String str() { return default_translation(); }

	public TranslatableComponent clone() {
		return new TranslatableComponent(key());
	}
}
