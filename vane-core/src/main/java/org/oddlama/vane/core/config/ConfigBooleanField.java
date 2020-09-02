package org.oddlama.vane.core.config;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigBooleanField extends ConfigField<Boolean> {
	private ConfigBoolean annotation;

	public ConfigBooleanField(Object owner, Field field, Function<String, String> map_name, ConfigBoolean annotation) {
		super(owner, field, map_name, "boolean", annotation.desc());
		this.annotation = annotation;
	}

	@Override
	public Boolean def() {
		final var override = overridden_def();
		if (override != null) {
			return override;
		} else {
			return annotation.def();
		}
	}

	@Override
	public void generate_yaml(StringBuilder builder, String indent) {
		append_description(builder, indent);
		append_default_value(builder, indent, def());
		append_field_definition(builder, indent, def());
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isBoolean(yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected boolean");
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.setBoolean(owner, yaml.getBoolean(yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

