package org.oddlama.vane.core.persistent;

import static org.reflections.ReflectionUtils.*;

import com.google.gson.Gson;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.lang.StringBuilder;
import java.nio.file.Files;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.json.simple.JSONObject;
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
import java.util.function.Consumer;

public class PersistentStorageManager {
	public class Migration {
		public long to;
		public String description;
		public Consumer<Map<String, Object>> migrator;

		public Migration(long to, String description, Consumer<Map<String, Object>> migrator) {
			this.to = to;
			this.description = description;
			this.migrator = migrator;
		}
	}

	private List<PersistentField> persistent_fields = new ArrayList<>();
	private List<Migration> migrations = new ArrayList<>();
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

	private void assert_field_prefix(Field field) {
		if (!field.getName().startsWith("storage_")) {
			throw new RuntimeException("Configuration fields must be named storage_. This is a bug.");
		}
	}

	private PersistentField compile_field(Object owner, Field field, Function<String, String> map_name) {
		assert_field_prefix(field);

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

	public void add_migration_to(long to, String description, Consumer<Map<String, Object>> migrator) {
		migrations.add(new Migration(to, description, migrator));
	}

	@SuppressWarnings("unchecked")
	public boolean load(File file) {
		if (!file.exists()) {
			if (is_loaded) {
				module.log.severe("Cannot reload persistent storage from nonexistent file '" + file.getName() + "'");
				return false;
			} else {
				// First start, variables will have their defaults.
				is_loaded = true;
				return true;
			}
		}

		// Reset loaded status
		is_loaded = false;

		// Open file and read json
		var content = "";
		try {
			content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			module.log.severe("error while loading persistent data from '" + file.getName() + "':");
			module.log.severe(e.getMessage());
			return false;
		}

		// Json to map
		var map = (HashMap<String, Object>)new Gson().fromJson(content, new TypeToken<HashMap<String, Object>>(){}.getType());

		// Check version and migrate if necessary
		// TODO migrate persistent storage

		try {
			for (var f : persistent_fields) {
				f.load(map);
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

		// Create map of all fields
		var map = new HashMap<String, Object>();
		for (var f : persistent_fields) {
			f.save(map);
		}

		// Map to json
		var json = new Gson().toJson(map);

		// Save to file
		try {
			Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			module.log.severe("error while saving persistent data!");
			module.log.severe(e.getMessage());
		}
	}
}
