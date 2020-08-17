package org.oddlama.imex.core.config;

import org.oddlama.imex.core.Module;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.apache.commons.lang.WordUtils;

import org.oddlama.imex.annotation.ConfigDouble;
import org.oddlama.imex.annotation.ConfigLong;
import org.oddlama.imex.annotation.ConfigString;
import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.annotation.LangString;
import org.oddlama.imex.annotation.LangVersion;

public class ConfigStringField extends ConfigField<String> {
	public ConfigString annotation;

	public ConfigStringField(Module module, Field field, ConfigString annotation) {
		super(module, field, String.class);
		this.annotation = annotation;
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		// Description
		append_description(builder, annotation.desc());

		// Default
		var def = "\"" + annotation.def().replace("\"", "\\\"") + "\"";
		append_default_value(builder, def);

		// Definition
		append_field_definition(builder, def);
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws LoadException {
		check_yaml_path(yaml);

		if (!yaml.isString(get_yaml_path())) {
			throw new LoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected string");
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

