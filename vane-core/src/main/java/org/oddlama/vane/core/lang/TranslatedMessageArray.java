package org.oddlama.vane.core.lang;

import java.util.ArrayList;
import java.util.List;

import org.oddlama.vane.core.module.Module;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

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

	public List<Component> format(Object... args) {
		final var arr = new ArrayList<Component>();
		for (int i = 0; i < default_translation.size(); ++i) {
			final var list = new ArrayList<ComponentLike>();
			for (final var o : args) {
				if (o instanceof ComponentLike) {
					list.add((ComponentLike)o);
				} else if (o instanceof String) {
					list.add(LegacyComponentSerializer.legacySection().deserialize((String)o));
				} else {
					throw new RuntimeException("Error while formatting message '" + key() + "', got invalid argument " + o);
				}
			}
			arr.add(Component.translatable(key + "." + i, list));
		}
		return arr;
	}
}
