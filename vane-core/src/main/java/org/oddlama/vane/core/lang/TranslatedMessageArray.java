package org.oddlama.vane.core.lang;

import java.util.ArrayList;
import java.util.List;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

import org.oddlama.vane.core.module.Module;

public class TranslatedMessageArray {
	private Module<?> module;
	private String key;
	private List<String> default_translation;

	public TranslatedMessageArray(final Module<?> module, final String key, final List<String> default_translation) {
		this.module = module;
		this.key = key;
		this.default_translation = default_translation;
	}

	public int size() { return default_translation.size(); }
	public String key() { return key; }
	public List<String> str(Object... args) {
		try {
			final var list = new ArrayList<String>();
			for (final var s : default_translation) {
				list.add(String.format(s, args));
			}
			return list;
		} catch (Exception e) {
			throw new RuntimeException("Error while formatting message '" + key() + "'", e);
		}
	}

	public List<BaseComponent> format(Object... args) {
		final var arr = new ArrayList<BaseComponent>();
		for (int i = 0; i < default_translation.size(); ++i) {
			arr.add(new TranslatableComponent(key + "." + i, args));
		}
		return arr;
	}
}
