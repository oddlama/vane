package org.oddlama.vane.core.persistent;

import static org.reflections.ReflectionUtils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.persistent.PersistentField;

public class PersistentStorageManager {
	public class Migration {
		public long to;
		public String name;
		public Consumer<Map<String, Object>> migrator;

		public Migration(long to, String name, Consumer<Map<String, Object>> migrator) {
			this.to = to;
			this.name = name;
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
			throw new RuntimeException("Configuration fields must be prefixed storage_. This is a bug.");
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

	public void add_migration_to(long to, String name, Consumer<Map<String, Object>> migrator) {
		migrations.add(new Migration(to, name, migrator));
	}

	@SuppressWarnings("unchecked")
	public boolean load(File file) {
		if (!file.exists() && is_loaded) {
			module.log.severe("Cannot reload persistent storage from nonexistent file '" + file.getName() + "'");
			return false;
		}

		// Reset loaded status
		is_loaded = false;

		// Declare map
		Map<String, Object> map;

		// Open file and read map
		if (file.exists()) {
			try (var ois = new ObjectInputStream(new FileInputStream(file))) {
				map = (Map<String, Object>)ois.readObject();
			} catch (IOException | ClassNotFoundException e) {
				module.log.severe("error while loading persistent data from '" + file.getName() + "':");
				module.log.severe(e.getMessage());
				return false;
			}
		} else {
			map = new HashMap<String, Object>();
		}

		// Check version and migrate if necessary
		final var version_path = module.storage_path_of("storage_version");
		final var version_obj = (Long)map.get(version_path);
		final var version = version_obj == null ? 0 : (long)version_obj;
		final var needed_version = module.annotation.storage_version();
		if (version != needed_version && migrations.size() > 0) {
			module.log.info("Persistent storage is out of date.");
			module.log.info("§dMigrating storage from version §b" + version + " → " + needed_version + "§d:");

			// Sort migrations by target version,
			// then apply new migrations in order.
			migrations.stream()
				.filter(m -> m.to >= version)
				.sorted((a, b) -> Long.compare(a.to, b.to))
				.forEach(m -> {
					module.log.info("  → §b" + m.to + "§r : Applying migration '§a" + m.name + "§r'");
					m.migrator.accept(map);
				});
		}

		// Save new version
		map.put(version_path, needed_version);

		try {
			for (var f : persistent_fields) {
				// If we have just initialized a new map, we only load values that
				// have defined keys (e.g. from initialization migrations)
				if (version == 0 && !map.containsKey(f.path())) {
					continue;
				}

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

		// Save to file
		try (var oos = new ObjectOutputStream(new FileOutputStream(file))) {
			oos.writeObject(map);
		} catch (IOException e) {
			module.log.severe("error while saving persistent data!");
			module.log.severe(e.getMessage());
		}
	}
}
