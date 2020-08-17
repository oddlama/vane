package org.oddlama.imex.core.config;

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

public class ConfigVersionField extends ConfigField<Long> {
	public ConfigVersion annotation;

	public ConfigVersionField(Field field, ConfigVersion annotation) {
		super(field);
		this.annotation = annotation;
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		// Description
		final var description = "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated.";
		append_description(builder, description);

		// Definition
		append_field_definition(builder, annotation.value());
	}

	@Override
	public Long load(YamlConfiguration yaml) {
		check_yaml_path(yaml);

		if (!yaml.isLong(name)) {
			throw new RuntimeException("Invalid type for yaml path '" + name + "', expected long");
		}
		return yaml.getLong(name);
	}
}

