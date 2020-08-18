package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigStringField extends ConfigField<String> {
	public ConfigString annotation;

	public ConfigStringField(Module module, Field field, ConfigString annotation) {
		super(module, field, "string");
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
			field.set(module, yaml.getString(get_yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

