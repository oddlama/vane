package org.oddlama.imex.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.imex.annotation.ConfigString;
import org.oddlama.imex.core.Module;
import org.oddlama.imex.core.YamlLoadException;

public class ConfigStringField extends ConfigField<String> {
	public ConfigString annotation;

	public ConfigStringField(Module module, Field field, ConfigString annotation) {
		super(module, field, String.class);
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

