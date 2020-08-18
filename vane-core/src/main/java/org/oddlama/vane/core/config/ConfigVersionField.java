package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigVersionField extends ConfigField<Long> {
	public ConfigVersion annotation;

	public ConfigVersionField(Module module, Field field, ConfigVersion annotation) {
		super(module, field, "version id");
		this.annotation = annotation;

		// Version field should be at the bottom
		this.sort_priority = 100;
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		final var description = "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated.";
		append_description(builder, description);
		append_field_definition(builder, annotation.value());
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!(yaml.get(get_yaml_path()) instanceof Number)) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected long");
		}

		var val = yaml.getLong(get_yaml_path());
		if (val < 1) {
			throw new YamlLoadException("Configuration '" + get_yaml_path() + "' has an invalid value: Value must be >= 1");
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

