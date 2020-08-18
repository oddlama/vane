package org.oddlama.vane.core.lang;

import static org.reflections.ReflectionUtils.*;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.LangMessage;
import org.oddlama.vane.annotation.LangString;
import org.oddlama.vane.annotation.LangVersion;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.YamlLoadException;
import org.oddlama.vane.core.lang.LangField;
import org.oddlama.vane.core.lang.LangMessageField;
import org.oddlama.vane.core.lang.LangStringField;
import org.oddlama.vane.core.lang.LangVersionField;

public class LangManager {
	private List<LangField<?>> lang_fields = new ArrayList<>();
	LangVersionField field_version;
	Module module;

	public LangManager(Module module) {
		this.module = module;
	}

	public long expected_version() {
		return field_version.annotation.value();
	}

	private boolean has_lang_annotation(Field field) {
		for (var a : field.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.Lang")) {
				return true;
			}
		}
		return false;
	}

	private void assert_field_prefix(Field field) {
		if (!field.getName().startsWith("lang_")) {
			throw new RuntimeException("Language fields must be named lang_. This is a bug.");
		}
	}

	private LangField<?> compile_field(Field field) {
		assert_field_prefix(field);

		// Get the annotation
		Annotation annotation = null;
		for (var a : field.getAnnotations()) {
			if (a.annotationType().getName().startsWith("org.oddlama.vane.annotation.Lang")) {
				if (annotation == null) {
					annotation = a;
				} else {
					throw new RuntimeException("Language fields must have exactly one annotation.");
				}
			}
		}
		assert annotation != null;
		final var atype = annotation.annotationType();

		// Return correct wrapper object
		if (atype.equals(LangString.class)) {
			return new LangStringField(module, field, (LangString)annotation);
		} else if (atype.equals(LangMessage.class)) {
			return new LangMessageField(module, field, (LangMessage)annotation);
		} else if (atype.equals(LangVersion.class)) {
			if (field_version != null) {
				throw new RuntimeException("There must be exactly one @LangVersion field! (found multiple)");
			}
			return field_version = new LangVersionField(module, field, (LangVersion)annotation);
		} else {
			throw new RuntimeException("Missing LangField handler for @" + atype.getName() + ". This is a bug.");
		}
	}

	@SuppressWarnings("unchecked")
	public void compile(Module module) {
		// Compile all annotated fields
		lang_fields = getAllFields(module.getClass()).stream()
			.filter(this::has_lang_annotation)
			.map(this::compile_field)
			.collect(Collectors.toList());

		if (field_version == null) {
			throw new RuntimeException("There must be exactly one @LangVersion field! (found none)");
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
				module.log.severe("This language file is for an older version of " + module.getName() + ".");
				module.log.severe("Please update your file or use an officially supported language file.");
			} else {
				module.log.severe("This language file is for a future version of " + module.getName() + ".");
				module.log.severe("Please use the correct file for this version, or use an officially");
				module.log.severe("supported language file.");
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
			// Check languration for errors
			for (var f : lang_fields) {
				f.check_loadable(yaml);
			}

			lang_fields.stream().forEach(f -> f.load(yaml));
		} catch (YamlLoadException e) {
			module.log.severe("error while loading '" + file.getName() + "':");
			module.log.severe(e.getMessage());
			return false;
		}
		return true;
	}
}
