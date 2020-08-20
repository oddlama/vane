package org.oddlama.vane.core.lang;

import static org.reflections.ReflectionUtils.*;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.YamlLoadException;

public class LangMessageField extends LangField<MessageFormat> {
	public LangMessage annotation;

	public LangMessageField(Object owner, Field field, Function<String, String> map_name, LangMessage annotation) {
		super(owner, field, map_name);
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
			field.set(owner, new MessageFormat(yaml.getString(get_yaml_path())));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

