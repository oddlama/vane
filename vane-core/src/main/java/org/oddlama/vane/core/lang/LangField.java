package org.oddlama.vane.core.lang;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.lang.ResourcePackTranslation;
import org.oddlama.vane.core.YamlLoadException;

public abstract class LangField<T> {
	protected Object owner;
	protected Field field;
	protected String name;
	private ResourcePackTranslation resource_pack_translation_annotation;

	public LangField(Object owner, Field field, Function<String, String> map_name) {
		this.owner = owner;
		this.field = field;
		this.name = map_name.apply(field.getName().substring("lang_".length()));
		this.resource_pack_translation_annotation = field.getAnnotation(ResourcePackTranslation.class);

		field.setAccessible(true);
	}

	public String get_name() {
		return name;
	}

	public String get_yaml_path() {
		return name;
	}

	protected void check_yaml_path(YamlConfiguration yaml) throws YamlLoadException {
		if (!yaml.contains(name, true)) {
			throw new YamlLoadException("yaml is missing entry with path '" + name + "'");
		}
	}

	public abstract void check_loadable(YamlConfiguration yaml) throws YamlLoadException;
	public abstract void load(YamlConfiguration yaml);

	@SuppressWarnings("unchecked")
	public T get() {
		try {
			return (T)field.get(owner);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}

	public boolean has_resource_pack_translation() {
		return resource_pack_translation_annotation != null;
	}

	public String resource_pack_translation_namespace() {
		if (resource_pack_translation_annotation == null) {
			return null;
		}

		return resource_pack_translation_annotation.namespace();
	}

	public String resource_pack_translation_key() {
		if (resource_pack_translation_annotation == null) {
			return null;
		}

		// Resolve dynamic overrides
		try {
			return (String)owner.getClass().getMethod(field.getName() + "_translation_key").invoke(owner);
		} catch (NoSuchMethodException e) {
			// Ignore, field wasn't overridden
			return resource_pack_translation_annotation.key();
		} catch (InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException("Could not call " + owner.getClass().getName() + "." + field.getName() + "_desc() to override description value", e);
		}
	}
}
