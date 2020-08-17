package org.oddlama.imex.core.config;

import static org.reflections.ReflectionUtils.*;

import java.io.File;
import java.lang.StringBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.imex.annotation.ConfigBoolean;
import org.oddlama.imex.annotation.ConfigDouble;
import org.oddlama.imex.annotation.ConfigInt;
import org.oddlama.imex.annotation.ConfigLong;
import org.oddlama.imex.annotation.ConfigMaterialSet;
import org.oddlama.imex.annotation.ConfigString;
import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.core.Module;
import org.oddlama.imex.core.YamlLoadException;
import org.oddlama.imex.core.config.ConfigBooleanField;
import org.oddlama.imex.core.config.ConfigDoubleField;
import org.oddlama.imex.core.config.ConfigField;
import org.oddlama.imex.core.config.ConfigIntField;
import org.oddlama.imex.core.config.ConfigLongField;
import org.oddlama.imex.core.config.ConfigMaterialSetField;
import org.oddlama.imex.core.config.ConfigStringField;
import org.oddlama.imex.core.config.ConfigVersionField;

public class ConfigManager {
	private List<ConfigField<?>> config_fields = new ArrayList<>();
	ConfigVersionField field_version;
	Module module;

	public ConfigManager(Module module) {
		this.module = module;
	}

	public long expected_version() {
		return field_version.annotation.value();
	}

	private boolean has_config_annotation(Field field) {
		for (var a : field.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.imex.annotation.Config")) {
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

	private ConfigField<?> compile_field(Field field) {
		assert_field_prefix(field);

		// Get the annotation
		Annotation annotation = null;
		for (var a : field.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.imex.annotation.Config")) {
				if (annotation == null) {
					annotation = a;
				} else {
					throw new RuntimeException("Configuration fields must have exactly one annotation.");
				}
			}
		}
		assert annotation != null;
		final var atype = annotation.annotationType();

		// Return correct wrapper object
		if (atype.equals(ConfigBoolean.class)) {
			return new ConfigBooleanField(module, field, (ConfigBoolean)annotation);
		} else if (atype.equals(ConfigDouble.class)) {
			return new ConfigDoubleField(module, field, (ConfigDouble)annotation);
		} else if (atype.equals(ConfigInt.class)) {
			return new ConfigIntField(module, field, (ConfigInt)annotation);
		} else if (atype.equals(ConfigLong.class)) {
			return new ConfigLongField(module, field, (ConfigLong)annotation);
		} else if (atype.equals(ConfigMaterialSet.class)) {
			return new ConfigMaterialSetField(module, field, (ConfigMaterialSet)annotation);
		} else if (atype.equals(ConfigString.class)) {
			return new ConfigStringField(module, field, (ConfigString)annotation);
		} else if (atype.equals(ConfigVersion.class)) {
			if (field_version != null) {
				throw new RuntimeException("There must be exactly one @ConfigVersion field! (found multiple)");
			}
			return field_version = new ConfigVersionField(module, field, (ConfigVersion)annotation);
		} else {
			throw new RuntimeException("Missing ConfigField handler for @" + atype.getName() + ". This is a bug.");
		}
	}

	@SuppressWarnings("unchecked")
	public void compile(Module module) {
		config_fields = getAllFields(module.getClass()).stream()
			.filter(this::has_config_annotation)
		// Compile all annotated fields
			.map(this::compile_field)
		// Sort fields alphabetically, and put version and lang first
			.sorted((a, b) -> a.compareTo(b))
			.collect(Collectors.toList());

		if (field_version == null) {
			throw new RuntimeException("There must be exactly one @ConfigVersion field! (found none)");
		}
	}

	public void generate_yaml(StringBuilder builder) {
		builder.append("# vim: set tabstop=2 softtabstop=0 expandtab shiftwidth=2:\n");

		config_fields.forEach(f -> {
			builder.append("\n");
			f.generate_yaml(builder);
		});
	}

	private boolean verify_version(File file, long version) {
		if (version != expected_version()) {
			module.log.severe(file.getName() + ": expected version " + expected_version() + ", but got " + version);

			if (version == 0) {
				module.log.severe("Something went wrong while generating or loading the configuration.");
				module.log.severe("If you are sure your configuration is correct and this isn't a file");
				module.log.severe("system permission issue, please report this to https://github.com/oddlama/imex/issues");
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

			config_fields.stream().forEach(f -> f.load(yaml));
		} catch (YamlLoadException e) {
			module.log.severe("error while loading '" + file.getName() + "':");
			module.log.severe(e.getMessage());
			return false;
		}
		return true;
	}
}
