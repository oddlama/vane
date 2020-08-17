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

import org.oddlama.imex.core.Module;
import org.oddlama.imex.annotation.ConfigDouble;
import org.oddlama.imex.annotation.ConfigLong;
import org.oddlama.imex.annotation.ConfigString;
import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.annotation.LangString;
import org.oddlama.imex.annotation.LangVersion;

public abstract class ConfigField<T> {
	public Field field;
	public String name;

	public ConfigField(Field field) {
		this.field = field;
		this.name = field.getName().substring("config_".length());
	}

	protected void append_description(StringBuilder builder, String description) {
		final var description_wrapped = "# " + WordUtils.wrap(description, 80, "\n# ", false);
		builder.append(description_wrapped);
		builder.append("\n");
	}

	protected void append_value_range(StringBuilder builder, T min, T max, T invalid_min, T invalid_max) {
		builder.append("# Valid values: ");
		if (min != invalid_min) {
			if (max != invalid_max) {
				builder.append(min);
				builder.append(" <= x <= ");
				builder.append(max);
			} else {
				builder.append(min);
				builder.append(" <= x");
			}
		} else {
			if (max != invalid_max) {
				builder.append("x <= ");
				builder.append(max);
				builder.append("\n");
			} else {
				builder.append("any double");
			}
		}
		builder.append("\n");
	}

	protected void append_default_value(StringBuilder builder, T def) {
		builder.append("# Default: ");
		builder.append(def);
		builder.append("\n");
	}

	protected void append_field_definition(StringBuilder builder, T def) {
		builder.append(name);
		builder.append(": ");
		builder.append(def);
		builder.append("\n");
	}

	public abstract void generate_yaml(StringBuilder builder);
	public abstract T load(YamlConfiguration yaml);

	//@SuppressWarnings("unchecked")
	//public T get(Module module) {
	//	try {
	//		return (T)field.get(module);
	//	} catch (IllegalAccessException e) {
	//		e.printStackTrace();
	//		throw new RuntimeException("Invalid field access!");
	//	}
	//}

	protected void check_yaml_path(YamlConfiguration yaml) {
		if (!yaml.contains(name, true)) {
			throw new RuntimeException("yaml is missing configuration with path '" + name + "'");
		}
	}
}
