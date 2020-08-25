package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import java.io.File;
import java.lang.StringBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.config.ConfigBooleanField;
import org.oddlama.vane.core.config.ConfigDoubleField;
import org.oddlama.vane.core.config.ConfigField;
import org.oddlama.vane.core.config.ConfigIntField;
import org.oddlama.vane.core.config.ConfigLongField;
import org.oddlama.vane.core.config.ConfigMaterialSetField;
import org.oddlama.vane.core.config.ConfigStringListMapField;
import org.oddlama.vane.core.config.ConfigStringField;
import org.oddlama.vane.core.config.ConfigVersionField;
import org.oddlama.vane.core.module.Module;

public class ConfigManager {
	private List<ConfigField<?>> config_fields = new ArrayList<>();
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
			throw new RuntimeException("Configuration fields must be named config_. This is a bug.");
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
			return new ConfigBooleanField(owner, field, map_name, (ConfigBoolean)annotation);
		} else if (atype.equals(ConfigDouble.class)) {
			return new ConfigDoubleField(owner, field, map_name, (ConfigDouble)annotation);
		} else if (atype.equals(ConfigInt.class)) {
			return new ConfigIntField(owner, field, map_name, (ConfigInt)annotation);
		} else if (atype.equals(ConfigLong.class)) {
			return new ConfigLongField(owner, field, map_name, (ConfigLong)annotation);
		} else if (atype.equals(ConfigMaterialSet.class)) {
			return new ConfigMaterialSetField(owner, field, map_name, (ConfigMaterialSet)annotation);
		} else if (atype.equals(ConfigString.class)) {
			return new ConfigStringField(owner, field, map_name, (ConfigString)annotation);
		} else if (atype.equals(ConfigStringListMap.class)) {
			return new ConfigStringListMapField(owner, field, map_name, (ConfigStringListMap)annotation);
		} else if (atype.equals(ConfigVersion.class)) {
			if (owner != module) {
				throw new RuntimeException("@ConfigVersion can only be used inside the main module. This is a bug.");
			}
			if (field_version != null) {
				throw new RuntimeException("There must be exactly one @ConfigVersion field! (found multiple). This is a bug.");
			}
			return field_version = new ConfigVersionField(owner, field, map_name, (ConfigVersion)annotation);
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
				module.log.severe("system permission issue, please report this to https://github.com/oddlama/vane/issues");
			} else if (version < expected_version()) {
				module.log.severe("This config is for an older version of " + module.getName() + ".");
				module.log.severe("Please backup the file and delete it afterwards. It will");
				module.log.severe("then be regenerated the next time the server is started.");
			} else {
				module.log.severe("This config is for a future version of " + module.getName() + ".");
				module.log.severe("Please use the correct file for this version, or delete it and");
				module.log.severe("it will be regenerated next time the server is started.");
			}

			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public void compile(Object owner, Function<String, String> map_name) {
		// Compile all annotated fields
		config_fields.addAll(getAllFields(owner.getClass()).stream()
			.filter(this::has_config_annotation)
			.map(f -> compile_field(owner, f, map_name))
			.collect(Collectors.toList()));

		// Sort fields alphabetically, and by precedence (e.g. put version last and lang first)
		Collections.sort(config_fields);

		if (owner == module && field_version == null) {
			throw new RuntimeException("There must be exactly one @ConfigVersion field! (found none). This is a bug.");
		}
	}

	private String indent_str(int level) {
		return "  ".repeat(level);
	}

	public void generate_yaml(StringBuilder builder) {
		builder.append("# vim: set tabstop=2 softtabstop=0 expandtab shiftwidth=2:\n");

		// Use the version field as a neutral field in the root group
		ConfigField<?> last_field = field_version;
		var indent = "";

		for (var f : config_fields) {
			builder.append("\n");

			if (!ConfigField.same_group(last_field, f)) {
				final var last_indent_level = last_field.group_count();
				final var new_indent_level = f.group_count();
				final var common_indent_level = ConfigField.common_group_count(last_field, f);
				for (int i = common_indent_level; i < new_indent_level; ++i) {
					indent = indent_str(i);
					builder.append(indent + f.components()[i] + ":\n");
				}

				indent = indent_str(new_indent_level);
			}

			// Append field yaml
			f.generate_yaml(builder, indent);
			last_field = f;
		}
	}

	public boolean reload(File file) {
		// Load file
		final var yaml = YamlConfiguration.loadConfiguration(file);

		// Check version
		final var version = yaml.getLong("version", -1);
		if (!verify_version(file, version)) {
			return false;
		}

		try {
			// Check configuration for errors
			for (var f : config_fields) {
				f.check_loadable(yaml);
			}

			for (var f : config_fields) {
				f.load(yaml);
			}
		} catch (YamlLoadException e) {
			module.log.severe("error while loading '" + file.getName() + "':");
			module.log.severe(e.getMessage());
			return false;
		}
		return true;
	}
}
