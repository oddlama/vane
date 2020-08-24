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
	protected String path;
	protected String type_name;
	protected int sort_priority = 0;

	private String[] yaml_path_components;
	private String yaml_group_path;
	private String basename;

	public ConfigField(Object owner, Field field, Function<String, String> map_name, String type_name) {
		this.owner = owner;
		this.field = field;
		this.path = map_name.apply(field.getName().substring("config_".length()));
		this.yaml_path_components = path.split("\\.");

		var last_dot = path.lastIndexOf(".");
		this.yaml_group_path = last_dot == -1 ? "" : path.substring(0, last_dot);

		this.basename = yaml_path_components[yaml_path_components.length - 1];
		this.type_name = type_name;

		// lang, enabled, metrics_enabled should be at the top
		if (this.path.equals("lang")) {
			this.sort_priority = -10;
		} else if (this.path.equals("enabled")) {
			this.sort_priority = -9;
		} else if (this.path.equals("metrics_enabled")) {
			this.sort_priority = -8;
		}

		field.setAccessible(true);
	}

	public String get_yaml_group_path() {
		return path;
	}

	public String yaml_path() {
		return path;
	}

	public String yaml_group_path() {
		return yaml_group_path;
	}

	public String basename() {
		return basename;
	}

	private String modify_yaml_path_for_sorting(String path) {
		// Enable fields should always be at the top, and therfore
		// get treated without the suffix.
		if (path.endsWith("_enabled")) {
			return path.substring(0, path.lastIndexOf("_enabled"));
		}
		return path;
	}

	@Override
	public int compareTo(ConfigField<?> other) {
		if (sort_priority != other.sort_priority) {
			return sort_priority - other.sort_priority;
		} else {
			for (int i = 0; i < Math.min(yaml_path_components.length, other.yaml_path_components.length); ++i) {
				var c = yaml_path_components[i].compareTo(other.yaml_path_components[i]);
				if (c != 0) {
					return c;
				}
			}
			return modify_yaml_path_for_sorting(yaml_path()).compareTo(
					modify_yaml_path_for_sorting(other.yaml_path()));
		}
	}

	protected void append_description(StringBuilder builder, String indent, String description) {
		final var description_wrapped = indent + "# " + WordUtils.wrap(description, 80, "\n" + indent + "# ", false);
		builder.append(description_wrapped);
		builder.append("\n");
	}

	protected void append_value_range(StringBuilder builder, String indent, T min, T max, T invalid_min, T invalid_max) {
		builder.append(indent);
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

	protected void append_default_value(StringBuilder builder, String indent, T def) {
		builder.append(indent);
		builder.append("# Default: ");
		builder.append(def);
		builder.append("\n");
	}

	protected void append_field_definition(StringBuilder builder, String indent, T def) {
		builder.append(indent);
		builder.append(basename);
		builder.append(": ");
		builder.append(def);
		builder.append("\n");
	}

	protected void check_yaml_path(YamlConfiguration yaml) throws YamlLoadException {
		if (!yaml.contains(path, true)) {
			throw new YamlLoadException("yaml is missing entry with path '" + path + "'");
		}
	}

	public abstract void generate_yaml(StringBuilder builder, String indent);
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

	public String[] components() {
		return yaml_path_components;
	}

	public int group_count() {
		return yaml_path_components.length - 1;
	}

	public static boolean same_group(ConfigField<?> a, ConfigField<?> b) {
		if (a.yaml_path_components.length != b.yaml_path_components.length) {
			return false;
		}
		for (int i = 0; i < a.yaml_path_components.length - 1; ++i) {
			if (!a.yaml_path_components[i].equals(b.yaml_path_components[i])) {
				return false;
			}
		}
		return true;
	}

	public static int common_group_count(ConfigField<?> a, ConfigField<?> b) {
		int i;
		for (i = 0; i < Math.min(a.yaml_path_components.length, b.yaml_path_components.length) - 1; ++i) {
			if (!a.yaml_path_components[i].equals(b.yaml_path_components[i])) {
				return i;
			}
		}
		return i;
	}
}
