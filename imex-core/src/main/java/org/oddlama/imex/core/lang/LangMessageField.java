package org.oddlama.imex.core.lang;

import static org.reflections.ReflectionUtils.*;

import java.lang.reflect.Field;
import java.text.MessageFormat;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.core.Module;
import org.oddlama.imex.core.YamlLoadException;

public class LangMessageField extends LangField<MessageFormat> {
	public LangMessage annotation;

	public LangMessageField(Module module, Field field, LangMessage annotation) {
		super(module, field, MessageFormat.class);
		this.annotation = annotation;
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isString(get_yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected string");
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.set(module, new MessageFormat(yaml.getString(get_yaml_path())));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

