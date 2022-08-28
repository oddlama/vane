package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.getAllFields;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.commons.lang.WordUtils;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.YamlConfiguration;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDict;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigIntList;
import org.oddlama.vane.annotation.config.ConfigItemStack;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigStringList;
import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;

public class ConfigManager {

	private List<ConfigField<?>> config_fields = new ArrayList<>();
	private Map<String, String> section_descriptions = new HashMap<>();
	ConfigVersionField field_version;
	Module<?> module;

	public ConfigManager(Module<?> module) {
		this.module = module;
		compile(module, s -> s);
	}

	public long expected_version() {
		return module.annotation.config_version();
	}

	private boolean has_config_annotation(Field field) {
		for (var a : field.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.config.Config")) {
				return true;
			}
		}
		return false;
	}

	private void assert_field_prefix(Field field) {
		if (!field.getName().startsWith("config_")) {
			throw new RuntimeException("Configuration fields must be prefixed config_. This is a bug.");
		}
	}

	private ConfigField<?> compile_field(Object owner, Field field, Function<String, String> map_name) {
		assert_field_prefix(field);

		// Get the annotation
		Annotation annotation = null;
		for (var a : field.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.config.Config")) {
				if (annotation == null) {
					annotation = a;
				} else {
					throw new RuntimeException("Configuration fields must have exactly one @Config annotation.");
				}
			}
		}
		assert annotation != null;
		final var atype = annotation.annotationType();

		// Return correct wrapper object
		if (atype.equals(ConfigBoolean.class)) {
			return new ConfigBooleanField(owner, field, map_name, (ConfigBoolean) annotation);
		} else if (atype.equals(ConfigDict.class)) {
			return new ConfigDictField(owner, field, map_name, (ConfigDict) annotation);
		} else if (atype.equals(ConfigDouble.class)) {
			return new ConfigDoubleField(owner, field, map_name, (ConfigDouble) annotation);
		} else if (atype.equals(ConfigDoubleList.class)) {
			return new ConfigDoubleListField(owner, field, map_name, (ConfigDoubleList) annotation);
		} else if (atype.equals(ConfigExtendedMaterial.class)) {
			return new ConfigExtendedMaterialField(owner, field, map_name, (ConfigExtendedMaterial) annotation);
		} else if (atype.equals(ConfigInt.class)) {
			return new ConfigIntField(owner, field, map_name, (ConfigInt) annotation);
		} else if (atype.equals(ConfigIntList.class)) {
			return new ConfigIntListField(owner, field, map_name, (ConfigIntList) annotation);
		} else if (atype.equals(ConfigItemStack.class)) {
			return new ConfigItemStackField(owner, field, map_name, (ConfigItemStack) annotation);
		} else if (atype.equals(ConfigLong.class)) {
			return new ConfigLongField(owner, field, map_name, (ConfigLong) annotation);
		} else if (atype.equals(ConfigMaterial.class)) {
			return new ConfigMaterialField(owner, field, map_name, (ConfigMaterial) annotation);
		} else if (atype.equals(ConfigMaterialMapMapMap.class)) {
			return new ConfigMaterialMapMapMapField(owner, field, map_name, (ConfigMaterialMapMapMap) annotation);
		} else if (atype.equals(ConfigMaterialSet.class)) {
			return new ConfigMaterialSetField(owner, field, map_name, (ConfigMaterialSet) annotation);
		} else if (atype.equals(ConfigString.class)) {
			return new ConfigStringField(owner, field, map_name, (ConfigString) annotation);
		} else if (atype.equals(ConfigStringList.class)) {
			return new ConfigStringListField(owner, field, map_name, (ConfigStringList) annotation);
		} else if (atype.equals(ConfigStringListMap.class)) {
			return new ConfigStringListMapField(owner, field, map_name, (ConfigStringListMap) annotation);
		} else if (atype.equals(ConfigVersion.class)) {
			if (owner != module) {
				throw new RuntimeException("@ConfigVersion can only be used inside the main module. This is a bug.");
			}
			if (field_version != null) {
				throw new RuntimeException(
					"There must be exactly one @ConfigVersion field! (found multiple). This is a bug."
				);
			}
			return field_version = new ConfigVersionField(owner, field, map_name, (ConfigVersion) annotation);
		} else {
			throw new RuntimeException("Missing ConfigField handler for @" + atype.getName() + ". This is a bug.");
		}
	}

	private boolean verify_version(File file, long version) {
		if (version != expected_version()) {
			module.log.severe(file.getName() + ": expected version " + expected_version() + ", but got " + version);

			if (version == 0) {
				module.log.severe("Something went wrong while generating or loading the configuration.");
				module.log.severe("If you are sure your configuration is correct and this isn't a file");
				module.log.severe(
					"system permission issue, please report this to https://github.com/oddlama/vane/issues"
				);
			} else if (version < expected_version()) {
				module.log.severe("This config is for an older version of " + module.getName() + ".");
				module.log.severe("Please update your configuration. A new default configuration");
				module.log.severe("has been generated as 'config.yml.new'. Alternatively you can");
				module.log.severe("delete your configuration to have a new one generated next time.");

				generate_file(new File(module.getDataFolder(), "config.yml.new"), null);
			} else {
				module.log.severe("This config is for a future version of " + module.getName() + ".");
				module.log.severe("Please use the correct file for this version, or delete it and");
				module.log.severe("it will be regenerated next time the server is started.");
			}

			return false;
		}

		return true;
	}

	public void add_section_description(String yaml_path, String description) {
		section_descriptions.put(yaml_path, description);
	}

	@SuppressWarnings("unchecked")
	public void compile(Object owner, Function<String, String> map_name) {
		// Compile all annotated fields
		config_fields.addAll(
				getAllFields(owner.getClass())
						.stream()
						.filter(this::has_config_annotation)
						.map(f -> compile_field(owner, f, map_name)).toList()
		);

		// Sort fields alphabetically, and by precedence (e.g. put version last and lang first)
		Collections.sort(config_fields);

		if (owner == module && field_version == null) {
			throw new RuntimeException("There must be exactly one @ConfigVersion field! (found none). This is a bug.");
		}
	}

	private String indent_str(int level) {
		return "  ".repeat(level);
	}

	public void generate_yaml(StringBuilder builder, YamlConfiguration existing_compatible_config) {
		builder.append("# vim: set tabstop=2 softtabstop=0 expandtab shiftwidth=2:\n");
		builder.append("# This config file will automatically be updated, as long\n");
		builder.append("# as there are no incompatible changes between versions.\n");
		builder.append("# This means that additional comments will not be preserved!\n");

		// Use the version field as a neutral field in the root group
		ConfigField<?> last_field = field_version;
		var indent = "";

		for (var f : config_fields) {
			builder.append("\n");

			if (!ConfigField.same_group(last_field, f)) {
				final var new_indent_level = f.group_count();
				final var common_indent_level = ConfigField.common_group_count(last_field, f);

				// Build full common section path
				var section_path = "";
				for (int i = 0; i < common_indent_level; ++i) {
					section_path = Context.append_yaml_path(section_path, f.components()[i], ".");
				}

				// For each unopened section
				for (int i = common_indent_level; i < new_indent_level; ++i) {
					indent = indent_str(i);

					// Get full section path
					section_path = Context.append_yaml_path(section_path, f.components()[i], ".");

					// Append section description, if given.
					final var section_desc = section_descriptions.get(section_path);
					if (section_desc != null) {
						final var description_wrapped = WordUtils.wrap(
							section_desc,
							Math.max(60, 80 - indent.length()),
							"\n" + indent + "# ",
							false
						);
						builder.append(indent);
						builder.append("# ");
						builder.append(description_wrapped);
						builder.append("\n");
					}

					// Append section
					final var section_name = f.components()[i];
					builder.append(indent);
					builder.append(section_name);
					builder.append(":\n");
				}

				indent = indent_str(new_indent_level);
			}

			// Append field yaml
			f.generate_yaml(builder, indent, existing_compatible_config);
			last_field = f;
		}
	}

	public File standard_file() {
		return new File(module.getDataFolder(), "config.yml");
	}

	public boolean generate_file(File file, YamlConfiguration existing_compatible_config) {
		final var builder = new StringBuilder();
		generate_yaml(builder, existing_compatible_config);
		final var content = builder.toString();

		// Save to tmp file, then move atomically to prevent corruption.
		final var tmp_file = new File(file.getAbsolutePath() + ".tmp");
		try {
			Files.writeString(tmp_file.toPath(), content);
		} catch (IOException e) {
			module.log.log(Level.SEVERE, "error while writing config file '" + file + "'", e);
			return false;
		}

		// Move atomically to prevent corruption.
		try {
			Files.move(
				tmp_file.toPath(),
				file.toPath(),
				StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.ATOMIC_MOVE
			);
		} catch (IOException e) {
			module.log.log(
				Level.SEVERE,
				"error while atomically replacing '" +
				file +
				"' with temporary file (very recent changes might be lost)!",
				e
			);
			return false;
		}

		return true;
	}

	public boolean reload(File file) {
		// Load file
		var yaml = YamlConfiguration.loadConfiguration(file);

		// Check version
		final var version = yaml.getLong("version", -1);
		if (!verify_version(file, version)) {
			return false;
		}

		// Upgrade config to include all necessary keys (version-compatible extensions)
		final var tmp_file = new File(module.getDataFolder(), "config.yml.tmp");
		if (!generate_file(tmp_file, yaml)) {
			return false;
		}

		// Move atomically to prevent corruption.
		try {
			Files.move(
				tmp_file.toPath(),
				standard_file().toPath(),
				StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.ATOMIC_MOVE
			);
		} catch (IOException e) {
			module.log.log(
				Level.SEVERE,
				"error while atomically replacing '" +
				standard_file() +
				"' with updated version. Please manually resolve the conflict (new file is named '" +
				tmp_file +
				"')",
				e
			);
			return false;
		}

		// Load newly written file
		yaml = YamlConfiguration.loadConfiguration(file);

		try {
			// Check configuration for errors
			for (var f : config_fields) {
				f.check_loadable(yaml);
			}

			for (var f : config_fields) {
				f.load(yaml);
			}
		} catch (YamlLoadException e) {
			module.log.log(Level.SEVERE, "error while loading '" + file.getName() + "'", e);
			return false;
		}

		return true;
	}

	public void register_metrics(Metrics metrics) {
		// Track config values. Fields automatically know whether they want to be tracked or not via the annotation.
		// By default, annotations use sensible defaults, so e.g. no strings will be tracked automatically, except
		// when explicitly requested (e.g. language).
		for (var f : config_fields) {
			f.register_metrics(metrics);
		}
	}
}
