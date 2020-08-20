package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigIntField extends ConfigField<Integer> {
	public ConfigInt annotation;

	public ConfigIntField(Object owner, Field field, Function<String, String> map_name, ConfigInt annotation) {
		super(owner, field, map_name, "int");
		this.annotation = annotation;
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		append_description(builder, annotation.desc());
		append_value_range(builder, annotation.min(), annotation.max(), Integer.MIN_VALUE, Integer.MAX_VALUE);
		append_default_value(builder, annotation.def());
		append_field_definition(builder, annotation.def());
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!(yaml.get(get_yaml_path()) instanceof Number)) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected int");
		}

		var val = yaml.getInt(get_yaml_path());
		if (annotation.min() != Integer.MIN_VALUE && val < annotation.min()) {
			throw new YamlLoadException("Configuration '" + get_yaml_path() + "' has an invalid value: Value must be >= " + annotation.min());
		}
		if (annotation.max() != Integer.MAX_VALUE && val > annotation.max()) {
			throw new YamlLoadException("Configuration '" + get_yaml_path() + "' has an invalid value: Value must be <= " + annotation.max());
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.setInt(owner, yaml.getInt(get_yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

