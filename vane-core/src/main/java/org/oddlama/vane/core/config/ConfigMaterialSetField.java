package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.MaterialUtil.material_from;
import static org.oddlama.vane.util.Util.namespaced_key;

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

	public ConfigMaterialSetField(
		Object owner,
		Field field,
		Function<String, String> map_name,
		ConfigMaterialSet annotation
	) {
		super(owner, field, map_name, "set of materials", annotation.desc());
		this.annotation = annotation;
	}

	private void append_material_set_definition(StringBuilder builder, String indent, String prefix) {
		append_list_definition(
			builder,
			indent,
			prefix,
			def(),
			(b, m) -> {
				b.append("\"");
				b.append(escape_yaml(m.getKey().getNamespace()));
				b.append(":");
				b.append(escape_yaml(m.getKey().getKey()));
				b.append("\"");
			}
		);
	}

	@Override
	public Set<Material> def() {
		final var override = overridden_def();
		if (override != null) {
			return override;
		} else {
			return new HashSet<>(Arrays.asList(annotation.def()));
		}
	}

	@Override
	public void generate_yaml(StringBuilder builder, String indent) {
		append_description(builder, indent);

		// Default
		builder.append(indent);
		builder.append("# Default:\n");
		append_material_set_definition(builder, indent, "# ");

		// Definition
		builder.append(indent);
		builder.append(basename());
		builder.append(":\n");
		append_material_set_definition(builder, indent, "");
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isList(yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected list");
		}

		for (var obj : yaml.getList(yaml_path())) {
			if (!(obj instanceof String)) {
				throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected string");
			}

			final var str = (String) obj;
			final var split = str.split(":");
			if (split.length != 2) {
				throw new YamlLoadException(
					"Invalid material entry in list '" + yaml_path() + "': '" + str + "' is not a valid namespaced key"
				);
			}

			final var mat = material_from(namespaced_key(split[0], split[1]));
			if (mat == null) {
				throw new YamlLoadException(
					"Invalid material entry in list '" + yaml_path() + "': '" + str + "' does not exist"
				);
			}
		}
	}

	public void load(YamlConfiguration yaml) {
		final var set = new HashSet<>();
		for (var obj : yaml.getList(yaml_path())) {
			final var split = ((String) obj).split(":");
			set.add(material_from(namespaced_key(split[0], split[1])));
		}

		try {
			field.set(owner, set);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}
