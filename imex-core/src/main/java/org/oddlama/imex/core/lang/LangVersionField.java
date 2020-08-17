package org.oddlama.imex.core.lang;

import java.lang.StringBuilder;
import org.oddlama.imex.core.YamlLoadException;
import org.oddlama.imex.core.Module;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.reflections.ReflectionUtils.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.apache.commons.lang.WordUtils;

import org.oddlama.imex.annotation.ConfigDouble;
import org.oddlama.imex.annotation.ConfigLong;
import org.oddlama.imex.annotation.ConfigString;
import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.annotation.LangString;
import org.oddlama.imex.annotation.LangVersion;
import org.oddlama.imex.core.Module;

public class LangVersionField extends LangField<Long> {
	public LangVersion annotation;

	public LangVersionField(Module module, Field field, LangVersion annotation) {
		super(module, field, Long.class);
		this.annotation = annotation;
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!(yaml.get(get_yaml_path()) instanceof Number)) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected long");
		}

		var val = yaml.getLong(get_yaml_path());
		if (val < 1) {
			throw new YamlLoadException("Entry '" + get_yaml_path() + "' has an invalid value: Value must be >= 1");
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.setLong(module, yaml.getLong(get_yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

