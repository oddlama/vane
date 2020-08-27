package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import org.apache.commons.lang.ArrayUtils;
import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigIntList;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigIntListField extends ConfigField<Map<String, List<String>>> {
	public ConfigIntList annotation;

	public ConfigIntListField(Object owner, Field field, Function<String, String> map_name, ConfigIntList annotation) {
		super(owner, field, map_name, "int list");
		this.annotation = annotation;
	}

	private void append_int_list_defintion(StringBuilder builder, String indent, String prefix) {
		append_list_definition(builder, indent, prefix, ArrayUtils.toObject(annotation.def()), (b, i) -> b.append(i));
	}

	@Override
	public void generate_yaml(StringBuilder builder, String indent) {
		append_description(builder, indent, annotation.desc());
		append_value_range(builder, indent, annotation.min(), annotation.max(), Integer.MIN_VALUE, Integer.MAX_VALUE);

		// Default
		builder.append(indent);
		builder.append("# Default:\n");
		append_int_list_defintion(builder, indent, "# ");

		// Definition
		builder.append(indent);
		builder.append(basename());
		builder.append(":\n");
		append_int_list_defintion(builder, indent, "");
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isList(yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected list");
		}

		for (var obj : yaml.getList(yaml_path())) {
			if (!(obj instanceof Number)) {
				throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected int");
			}

			var val = yaml.getInt(yaml_path());
			if (annotation.min() != Integer.MIN_VALUE && val < annotation.min()) {
				throw new YamlLoadException("Configuration '" + yaml_path() + "' has an invalid value: Value must be >= " + annotation.min());
			}
			if (annotation.max() != Integer.MAX_VALUE && val > annotation.max()) {
				throw new YamlLoadException("Configuration '" + yaml_path() + "' has an invalid value: Value must be <= " + annotation.max());
			}
		}
	}

	public void load(YamlConfiguration yaml) {
		final var list = new ArrayList<Integer>();
		for (var obj : yaml.getList(yaml_path())) {
			list.add(((Number)obj).intValue());
		}

		try {
			field.set(owner, list);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

