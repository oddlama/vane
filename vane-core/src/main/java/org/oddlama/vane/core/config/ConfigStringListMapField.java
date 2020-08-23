package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigStringListMapField extends ConfigField<Map<String, List<String>>> {
	public ConfigStringListMap annotation;

	public ConfigStringListMapField(Object owner, Field field, Function<String, String> map_name, ConfigStringListMap annotation) {
		super(owner, field, map_name, "map of string lists");
		this.annotation = annotation;
	}

	private void append_string_list_map_defintion(StringBuilder builder, String indent, String prefix) {
		Arrays.stream(annotation.def()).forEach(map_entry -> {
			builder.append(indent);
			builder.append(prefix);
			builder.append("  ");
			builder.append(map_entry.key());
			builder.append(":\n");

			Arrays.stream(map_entry.list()).forEach(s -> {
				builder.append(indent);
				builder.append(prefix);
				builder.append("    - ");
				builder.append(s);
				builder.append("\n");
			});
		});
	}

	@Override
	public void generate_yaml(StringBuilder builder, String indent) {
		append_description(builder, indent, annotation.desc());

		// Default
		builder.append(indent);
		builder.append("# Default:\n");
		append_string_list_map_defintion(builder, indent, "# ");

		// Definition
		builder.append(indent);
		builder.append(basename());
		builder.append(":\n");
		append_string_list_map_defintion(builder, indent, "");
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isConfigurationSection(yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected group");
		}

		for (var list_key : yaml.getConfigurationSection(yaml_path()).getKeys(false)) {
			final var list_path = yaml_path() + "." + list_key;
			if (!yaml.isList(list_path)) {
				throw new YamlLoadException("Invalid type for yaml path '" + list_path + "', expected list");
			}

			for (var obj : yaml.getList(list_path)) {
				if (!(obj instanceof String)) {
					throw new YamlLoadException("Invalid type for yaml path '" + list_path + "', expected string");
				}
			}
		}
	}

	public void load(YamlConfiguration yaml) {
		final var map = new HashMap<String, List<String>>();
		for (var list_key : yaml.getConfigurationSection(yaml_path()).getKeys(false)) {
			final var list_path = yaml_path() + "." + list_key;
			final var list = new ArrayList<String>();
			map.put(list_key, list);
			for (var obj : yaml.getList(list_path)) {
				list.add((String)obj);
			}
		}

		try {
			field.set(owner, map);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

