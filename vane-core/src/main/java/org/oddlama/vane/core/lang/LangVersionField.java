package org.oddlama.vane.core.lang;

import static org.reflections.ReflectionUtils.*;

import java.lang.reflect.Field;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.YamlLoadException;

public class LangVersionField extends LangField<Long> {
	public LangVersion annotation;

	public LangVersionField(Module module, Field field, LangVersion annotation) {
		super(module, field);
		this.annotation = annotation;
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!(yaml.get(get_yaml_path()) instanceof Number)) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected long");
		}

		var val = yaml.getLong(get_yaml_path());
		if (val < 1) {
			throw new YamlLoadException("Entry '" + get_yaml_path() + "' has an invalid value: Value must be >= 1");
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.setLong(module, yaml.getLong(get_yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

