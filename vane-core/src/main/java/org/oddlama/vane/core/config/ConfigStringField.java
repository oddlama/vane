package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigStringField extends ConfigField<String> {
	public ConfigString annotation;

	public ConfigStringField(Object owner, Field field, Function<String, String> map_name, ConfigString annotation) {
		super(owner, field, map_name, "string");
		this.annotation = annotation;
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		append_description(builder, annotation.desc());
		var def = "\"" + annotation.def().replace("\"", "\\\"") + "\"";
		append_default_value(builder, def);
		append_field_definition(builder, def);
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isString(get_yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected string");
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.set(owner, yaml.getString(get_yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

