package org.oddlama.imex.core.config;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.text.MessageFormat;
import org.apache.commons.lang.ClassUtils;

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
	protected Module module;
	protected Field field;
	protected String name;
	protected Class<?> cls;
	protected int sort_priority = 0;

	public ConfigField(Module module, Field field, Class<?> cls) {
		this.module = module;
		this.field = field;
		this.name = field.getName().substring("config_".length());
		this.cls = cls;

		// lang should be at the top
		if (this.name.equals("lang")) {
			this.sort_priority = -10;
		}

		field.setAccessible(true);
	}

	public String get_yaml_path() {
		return name;
	}

	public int compareTo(ConfigField<?> other) {
		if (sort_priority != other.sort_priority) {
			return sort_priority - other.sort_priority;
		} else {
			return get_yaml_path().compareTo(other.get_yaml_path());
		}
	}

	protected void append_description(StringBuilder builder, String description) {
		final var description_wrapped = "# " + WordUtils.wrap(description, 80, "\n# ", false);
		builder.append(description_wrapped);
		builder.append("\n");
	}

	protected void append_value_range(StringBuilder builder, T min, T max, T invalid_min, T invalid_max) {
		builder.append("# Valid values: ");
		if (!min.equals(invalid_min)) {
			if (!max.equals(invalid_max)) {
				builder.append("[");
				builder.append(min);
				builder.append(",");
				builder.append(max);
				builder.append("]");
			} else {
				builder.append("[");
				builder.append(min);
				builder.append(",)");
			}
		} else {
			if (!max.equals(invalid_max)) {
				builder.append("(,");
				builder.append(max);
				builder.append("]");
			} else {
				var primitive_cls = ClassUtils.wrapperToPrimitive(cls);
				if (primitive_cls != null) {
					builder.append("Any " + primitive_cls.getName());
				} else {
					throw new RuntimeException("Unhandeled configuration type " + cls.getName() + " in append_value_range. This is a bug.");
				}
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

	protected void check_yaml_path(YamlConfiguration yaml) throws LoadException {
		if (!yaml.contains(name, true)) {
			throw new LoadException("yaml is missing configuration with path '" + name + "'");
		}
	}

	public abstract void generate_yaml(StringBuilder builder);
	public abstract void check_loadable(YamlConfiguration yaml) throws LoadException;
	public abstract void load(YamlConfiguration yaml);
}
