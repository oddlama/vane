package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.material_from;
import static org.oddlama.vane.util.Util.namespaced_key;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigMaterialSetField extends ConfigField<Set<Material>> {
	public ConfigMaterialSet annotation;

	public ConfigMaterialSetField(Object owner, Field field, Function<String, String> map_name, ConfigMaterialSet annotation) {
		super(owner, field, map_name, "set of materials");
		this.annotation = annotation;
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		append_description(builder, annotation.desc());

		// Default
		builder.append("# Default:\n");
		Arrays.stream(annotation.def())
			.forEach(m -> {
				builder.append("#  - \"");
				builder.append(m.getKey().getNamespace());
				builder.append(":");
				builder.append(m.getKey().getKey());
				builder.append("\"\n");
			});

		// Definition
		builder.append(name);
		builder.append(":\n");
		Arrays.stream(annotation.def())
			.forEach(m -> {
				builder.append("  - \"");
				builder.append(m.getKey().getNamespace());
				builder.append(":");
				builder.append(m.getKey().getKey());
				builder.append("\"\n");
			});
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isList(get_yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected list");
		}

		for (var obj : yaml.getList(get_yaml_path())) {
			if (!(obj instanceof String)) {
				throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected list");
			}

			final var str = (String)obj;
			final var split = str.split(":");
			if (split.length != 2) {
				throw new YamlLoadException("Invalid material entry in list '" + get_yaml_path() + "': '" + str + "' is not a valid namespaced key");
			}

			final var mat = material_from(namespaced_key(split[0], split[1]));
			if (mat == null) {
				throw new YamlLoadException("Invalid material entry in list '" + get_yaml_path() + "': '" + str + "' does not exist");
			}
		}
	}

	public void load(YamlConfiguration yaml) {
		final var set = new HashSet<>();
		for (var obj : yaml.getList(get_yaml_path())) {
			final var split = ((String)obj).split(":");
			set.add(material_from(namespaced_key(split[0], split[1])));
		}

		try {
			field.set(owner, set);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

