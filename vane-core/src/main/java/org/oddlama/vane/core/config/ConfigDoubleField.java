package org.oddlama.vane.core.config;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigDoubleField extends ConfigField<Double> {
	public ConfigDouble annotation;

	public ConfigDoubleField(Object owner, Field field, Function<String, String> map_name, ConfigDouble annotation) {
		super(owner, field, map_name, "double", annotation.desc());
		this.annotation = annotation;
	}

	@Override
	public Double def() {
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
		append_value_range(builder, indent, annotation.min(), annotation.max(), Double.NaN, Double.NaN);
		append_default_value(builder, indent, def());
		append_field_definition(builder, indent, def());
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isDouble(yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected double");
		}

		var val = yaml.getDouble(yaml_path());
		if (annotation.min() != Double.NaN && val < annotation.min()) {
			throw new YamlLoadException("Configuration '" + yaml_path() + "' has an invalid value: Value must be >= " + annotation.min());
		}
		if (annotation.max() != Double.NaN && val > annotation.max()) {
			throw new YamlLoadException("Configuration '" + yaml_path() + "' has an invalid value: Value must be <= " + annotation.max());
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.setDouble(owner, yaml.getDouble(yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

