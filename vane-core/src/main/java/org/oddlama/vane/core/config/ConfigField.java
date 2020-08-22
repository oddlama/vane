package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.Comparable;
import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.function.Function;

import org.apache.commons.lang.WordUtils;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.core.YamlLoadException;

public abstract class ConfigField<T> implements Comparable<ConfigField<?>> {
	protected Object owner;
	protected Field field;
	protected String name;
	protected String type_name;
	protected int sort_priority = 0;

	public ConfigField(Object owner, Field field, Function<String, String> map_name, String type_name) {
		this.owner = owner;
		this.field = field;
		this.name = map_name.apply(field.getName().substring("config_".length()));
		this.type_name = type_name;

		if (this.name.equals("lang")) {
			// lang should be at the top
			this.sort_priority = -10;
		} else if (this.name.equals("enabled")) {
			// enabled should be just below lang
			this.sort_priority = -9;
		}

		field.setAccessible(true);
	}

	public String get_name() {
		return name;
	}

	public String get_yaml_path() {
		return name;
	}

	private String modify_yaml_path_for_sorting(String path) {
		// Enable fields should always be at the top, and therfore
		// get treated without the suffix.
		if (path.endsWith("_enabled")) {
			return path.substring("_enabled".length());
		}
		return path;
	}

	@Override
	public int compareTo(ConfigField<?> other) {
		if (sort_priority != other.sort_priority) {
			return sort_priority - other.sort_priority;
		} else {
			return modify_yaml_path_for_sorting(get_yaml_path()).compareTo(
					modify_yaml_path_for_sorting(other.get_yaml_path()));
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
				builder.append("Any " + type_name);
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

	protected void check_yaml_path(YamlConfiguration yaml) throws YamlLoadException {
		if (!yaml.contains(name, true)) {
			throw new YamlLoadException("yaml is missing entry with path '" + name + "'");
		}
	}

	public abstract void generate_yaml(StringBuilder builder);
	public abstract void check_loadable(YamlConfiguration yaml) throws YamlLoadException;
	public abstract void load(YamlConfiguration yaml);

	@SuppressWarnings("unchecked")
	public T get() {
		try {
			return (T)field.get(owner);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}
