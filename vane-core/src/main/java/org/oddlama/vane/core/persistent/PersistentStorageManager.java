package org.oddlama.vane.core.persistent;

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

import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.annotation.config.ConfigStringListMap;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.persistent.PersistentField;
import org.oddlama.vane.core.config.ConfigDoubleField;
import org.oddlama.vane.core.config.ConfigField;
import org.oddlama.vane.core.config.ConfigIntField;
import org.oddlama.vane.core.config.ConfigLongField;
import org.oddlama.vane.core.config.ConfigMaterialSetField;
import org.oddlama.vane.core.config.ConfigStringListMapField;
import org.oddlama.vane.core.config.ConfigStringField;
import org.oddlama.vane.core.config.ConfigVersionField;
import org.oddlama.vane.core.module.Module;

public class PersistentStorageManager {
	private List<PersistentField> persistent_fields = new ArrayList<>();
	Module<?> module;
	boolean is_loaded = false;

	public PersistentStorageManager(Module<?> module) {
		this.module = module;
		compile(module, s -> s);
	}

	private boolean has_persistent_annotation(Field field) {
		for (var a : field.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.persistent.Persistent")) {
				return true;
			}
		}
		return false;
	}

	private PersistentField compile_field(Object owner, Field field, Function<String, String> map_name) {
		// Get the annotation
		Annotation annotation = null;
		for (var a : field.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.persistent.Persistent")) {
				if (annotation == null) {
					annotation = a;
				} else {
					throw new RuntimeException("Persistent fields must have exactly one @Persistent annotation.");
				}
			}
		}
		assert annotation != null;
		final var atype = annotation.annotationType();

		// Return correct wrapper object
		if (atype.equals(Persistent.class)) {
			return new PersistentField(owner, field, map_name);
		} else {
			throw new RuntimeException("Missing PersistentField handler for @" + atype.getName() + ". This is a bug.");
		}
	}

	@SuppressWarnings("unchecked")
	public void compile(Object owner, Function<String, String> map_name) {
		// Compile all annotated fields
		persistent_fields.addAll(getAllFields(owner.getClass()).stream()
			.filter(this::has_persistent_annotation)
			.map(f -> compile_field(owner, f, map_name))
			.collect(Collectors.toList()));
	}

	public boolean load(File file) {

		is_loaded = false;

		// Load file

		// Check version

		try {
			for (var f : persistent_fields) {
				f.load();
			}
		} catch (LoadException e) {
			module.log.severe("error while loading persistent variables from '" + file.getName() + "':");
			module.log.severe(e.getMessage());
			return false;
		}

		is_loaded = true;
		return true;
	}

	public void save(File file) {
		if (!is_loaded) {
			// Don't save if never loaded or a previous load was faulty.
			return;
		}
	}
}
