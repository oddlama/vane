package org.oddlama.imex.core;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.*;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.imex.annotation.ConfigDouble;
import org.oddlama.imex.annotation.ConfigLong;
import org.oddlama.imex.annotation.ConfigString;
import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.annotation.LangString;
import org.oddlama.imex.annotation.LangVersion;

public class ConfigManager {
	private class ConfigField {
		public Field field;
	}

	private List<ConfigField> config_fields = new ArrayList<>();

	public long expected_version() {
		return 1;
	}

	private boolean has_config_annotation(Field f) {
		for (var a : f.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.imex.annotation.Config")) {
				return true;
			}
		}
		return false;
	}

	private void assert_field_prefix(Field f) {
		if (!f.getName().startsWith("config_")) {
			throw new IllegalArgumentException("Configuration fields must be named config_. This is a bug.");
		}
	}

	private ConfigField compile_field(Field field) {
		assert_field_prefix(field);

		// Get the annotation
		Annotation annotation = null;
		for (var a : f.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.imex.annotation.Config")) {
				if (annotation == null) {
					annotation = a;
				} else {
					throw new IllegalArgumentException("Configuration fields must have exactly one annotation.");
				}
			}
		}
		assert annotation != null;
		var atype = annotation.annotationType();

		if (atype instanceof ConfigDouble) {
		} else if (atype instanceof ConfigLong) {
		} else if (atype instanceof ConfigString) {
		} else if (atype instanceof ConfigVersion) {
		} else if (atype instanceof LangMessage) {
		} else if (atype instanceof LangString) {
		} else if (atype instanceof LangVersion) {
		}

		return cf;
	}

	@SuppressWarnings("unchecked")
	public void compile(Module module) {
		var fields = getAllFields(module.getClass()).stream()
			.filter(this::has_config_annotation);
		config_fields = fields.map(this::compile_field).collect(Collectors.toList());
	}

	public void generate_yaml(StringBuilder builder) {
	}

	public boolean reload(Logger log, YamlConfiguration yaml) {
		return true;
	}
}
