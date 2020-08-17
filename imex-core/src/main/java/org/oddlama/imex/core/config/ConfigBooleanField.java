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

import org.oddlama.imex.core.YamlLoadException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.apache.commons.lang.WordUtils;

import org.oddlama.imex.annotation.ConfigDouble;
import org.oddlama.imex.annotation.ConfigLong;
import org.oddlama.imex.annotation.ConfigBoolean;
import org.oddlama.imex.annotation.ConfigString;
import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.annotation.LangString;
import org.oddlama.imex.annotation.LangVersion;

public class ConfigBooleanField extends ConfigField<Boolean> {
	public ConfigBoolean annotation;

	public ConfigBooleanField(Module module, Field field, ConfigBoolean annotation) {
		super(module, field, Boolean.class);
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

		if (!(yaml.get(get_yaml_path()) instanceof Number)) {
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

