package org.oddlama.vane.core.lang;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.core.YamlLoadException;

public abstract class LangField<T> {
	protected Object owner;
	protected Field field;
	protected String name;

	public LangField(Object owner, Field field, Function<String, String> map_name) {
		this.owner = owner;
		this.field = field;
		this.name = map_name.apply(field.getName().substring("lang_".length()));

		field.setAccessible(true);
	}

	public String get_name() {
		return name;
	}

	public String yaml_path() {
		return name;
	}

	protected void check_yaml_path(YamlConfiguration yaml) throws YamlLoadException {
		if (!yaml.contains(name, true)) {
			throw new YamlLoadException("yaml is missing entry with path '" + name + "'");
		}
	}

	public String key(final String namespace) {
		return namespace + "." + yaml_path();
	}

	public abstract void check_loadable(YamlConfiguration yaml) throws YamlLoadException;
	public abstract void load(final String namespace, final YamlConfiguration yaml);
	public abstract String str(final YamlConfiguration yaml);

	@SuppressWarnings("unchecked")
	public T get() {
		try {
			return (T)field.get(owner);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}
