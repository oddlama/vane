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

public class ConfigLongField extends ConfigField<Long> {
	public ConfigLong annotation;

	public ConfigLongField(Field field, ConfigLong annotation) {
		super(field);
		this.annotation = annotation;
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		// Description
		append_description(builder, annotation.desc());

		// Valid values
		append_value_range(builder, annotation.min(), annotation.max(), Long.MIN_VALUE, Long.MAX_VALUE);

		// Default
		append_default_value(builder, annotation.def());

		// Definition
		append_field_definition(builder, annotation.def());
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

