package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.ConfigBoolean;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigBooleanField extends ConfigField<Boolean> {
	public ConfigBoolean annotation;

	public ConfigBooleanField(Module module, Field field, ConfigBoolean annotation) {
		super(module, field, "boolean");
		this.annotation = annotation;
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		append_description(builder, annotation.desc());
		append_default_value(builder, annotation.def());
		append_field_definition(builder, annotation.def());
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isBoolean(get_yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected boolean");
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.setBoolean(module, yaml.getBoolean(get_yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

