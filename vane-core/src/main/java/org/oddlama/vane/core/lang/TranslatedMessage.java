package org.oddlama.vane.core.lang;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

public class TranslatedMessage extends TranslatedComponent {
	public TranslatedMessage(final String key, final String default_translation) {
		super(key, default_translation);
	}

	public String str(Object... args) {
		return String.format(default_translation(), args);
	}

	public TranslatableComponent format(Object... args) {
		return new TranslatableComponent(key(), args);
	}

	public void broadcast(final World world, Object... args) {
	}

	public void send(final CommandSender sender, Object... args) {
	}
}
