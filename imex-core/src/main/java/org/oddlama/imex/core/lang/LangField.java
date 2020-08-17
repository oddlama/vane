package org.oddlama.imex.core.lang;

import static org.reflections.ReflectionUtils.*;

import java.lang.reflect.Field;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.imex.core.Module;
import org.oddlama.imex.core.YamlLoadException;

public abstract class LangField<T> {
	protected Module module;
	protected Field field;
	protected String name;

	public LangField(Module module, Field field) {
		this.module = module;
		this.field = field;
		this.name = field.getName().substring("lang_".length());

		field.setAccessible(true);
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
}
