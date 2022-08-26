package org.oddlama.vane.core.config;

import static org.oddlama.vane.util.StorageUtil.namespaced_key;

import java.lang.reflect.Field;
import java.util.function.Function;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.material.ExtendedMaterial;

public class ConfigExtendedMaterialField extends ConfigField<ExtendedMaterial> {

	public ConfigExtendedMaterial annotation;

	public ConfigExtendedMaterialField(
		Object owner,
		Field field,
		Function<String, String> map_name,
		ConfigExtendedMaterial annotation
	) {
		super(owner, field, map_name, "extended material", annotation.desc());
		this.annotation = annotation;
	}

	@Override
	public ExtendedMaterial def() {
		final var override = overridden_def();
		if (override != null) {
			return override;
		} else {
			final var split = annotation.def().split(":");
			if (split.length != 2) {
				throw new RuntimeException(
					"Invalid default extended material entry for '" +
					yaml_path() +
					"': '" +
					annotation.def() +
					"' is not a valid namespaced key"
				);
			}
			return ExtendedMaterial.from(namespaced_key(split[0], split[1]));
		}
	}

	@Override
	public boolean metrics() {
		final var override = overridden_metrics();
		if (override != null) {
			return override;
		} else {
			return annotation.metrics();
		}
	}

	@Override
	public void generate_yaml(StringBuilder builder, String indent, YamlConfiguration existing_compatible_config) {
		append_description(builder, indent);
		append_default_value(
			builder,
			indent,
			"\"" + escape_yaml(def().key().getNamespace()) + ":" + escape_yaml(def().key().getKey()) + "\""
		);
		final var def = existing_compatible_config != null && existing_compatible_config.contains(yaml_path())
			? load_from_yaml(existing_compatible_config)
			: def();
		append_field_definition(
			builder,
			indent,
			"\"" + escape_yaml(def.key().getNamespace()) + ":" + escape_yaml(def.key().getKey()) + "\""
		);
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isString(yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + yaml_path() + "', expected string");
		}

		final var str = yaml.getString(yaml_path());
		final var split = str.split(":");
		if (split.length != 2) {
			throw new YamlLoadException(
				"Invalid extended material entry in list '" +
				yaml_path() +
				"': '" +
				str +
				"' is not a valid namespaced key"
			);
		}

		final var mat = ExtendedMaterial.from(namespaced_key(split[0], split[1]));
		if (mat == null) {
			throw new YamlLoadException(
				"Invalid extended material entry in list '" + yaml_path() + "': '" + str + "' does not exist"
			);
		}
	}

	public ExtendedMaterial load_from_yaml(YamlConfiguration yaml) {
		final var split = yaml.getString(yaml_path()).split(":");
		return ExtendedMaterial.from(namespaced_key(split[0], split[1]));
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.set(owner, load_from_yaml(yaml));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}
