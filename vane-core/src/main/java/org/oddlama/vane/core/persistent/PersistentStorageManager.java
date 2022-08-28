package org.oddlama.vane.core.persistent;

import static org.reflections.ReflectionUtils.getAllFields;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.module.Module;

public class PersistentStorageManager {

	public class Migration {

		public long to;
		public String name;
		public Consumer<JSONObject> migrator;

		public Migration(long to, String name, Consumer<JSONObject> migrator) {
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
		persistent_fields.addAll(
				getAllFields(owner.getClass())
						.stream()
						.filter(this::has_persistent_annotation)
						.map(f -> compile_field(owner, f, map_name)).toList()
		);
	}

	public void add_migration_to(long to, String name, Consumer<JSONObject> migrator) {
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

		final JSONObject json;
		if (file.exists()) {
			// Open file and read json
			try {
				json = new JSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8));
			} catch (IOException e) {
				module.log.severe("error while loading persistent data from '" + file.getName() + "':");
				module.log.severe(e.getMessage());
				return false;
			}
		} else {
			json = new JSONObject();
		}

		// Check version and migrate if necessary
		final var version_path = module.storage_path_of("storage_version");
		final var version = Long.parseLong(json.optString(version_path, "0"));
		final var needed_version = module.annotation.storage_version();
		if (version != needed_version && migrations.size() > 0) {
			module.log.info("Persistent storage is out of date.");
			module.log.info("§dMigrating storage from version §b" + version + " → " + needed_version + "§d:");

			// Sort migrations by target version,
			// then apply new migrations in order.
			migrations
				.stream()
				.filter(m -> m.to >= version)
				.sorted((a, b) -> Long.compare(a.to, b.to))
				.forEach(m -> {
					module.log.info("  → §b" + m.to + "§r : Applying migration '§a" + m.name + "§r'");
					m.migrator.accept(json);
				});
		}

		// Overwrite new version
		json.put(version_path, String.valueOf(needed_version));

		try {
			for (final var f : persistent_fields) {
				// If we have just initialized a new json object, we only load values that
				// have defined keys (e.g. from initialization migrations)
				if (version == 0 && !json.has(f.path())) {
					continue;
				}

				f.load(json);
			}
		} catch (IOException e) {
			module.log.log(Level.SEVERE, "error while loading persistent variables from '" + file.getName() + "'", e);
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

		// Create json with whole content
		final var json = new JSONObject();

		// Save version
		final var version_path = module.storage_path_of("storage_version");
		json.put(version_path, String.valueOf(module.annotation.storage_version()));

		// Save fields
		for (final var f : persistent_fields) {
			try {
				f.save(json);
			} catch (IOException e) {
				module.log.log(Level.SEVERE, "error while serializing persistent data!", e);
			}
		}

		// Save to tmp file, then move atomically to prevent corruption.
		final var tmp_file = new File(file.getAbsolutePath() + ".tmp");
		try {
			Files.writeString(tmp_file.toPath(), json.toString());
		} catch (IOException e) {
			module.log.log(Level.SEVERE, "error while saving persistent data to temporary file!", e);
			return;
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
		}
	}
}
