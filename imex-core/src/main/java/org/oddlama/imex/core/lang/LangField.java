package org.oddlama.imex.core.lang;

import org.oddlama.imex.core.YamlLoadException;
import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.text.MessageFormat;
import org.apache.commons.lang.ClassUtils;

import static org.reflections.ReflectionUtils.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.apache.commons.lang.WordUtils;

import org.oddlama.imex.core.Module;
import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.annotation.LangString;
import org.oddlama.imex.annotation.LangVersion;

public abstract class LangField<T> {
	protected Module module;
	protected Field field;
	protected String name;
	protected Class<?> cls;

	public LangField(Module module, Field field, Class<?> cls) {
		this.module = module;
		this.field = field;
		this.name = field.getName().substring("lang_".length());
		this.cls = cls;

		field.setAccessible(true);
	}

	public String get_yaml_path() {
		return name;
	}

	protected void check_yaml_path(YamlConfiguration yaml) throws YamlLoadException {
		if (!yaml.contains(name, true)) {
			throw new YamlLoadException("yaml is missing entry with path '" + name + "'");
		}
	}

	public abstract void check_loadable(YamlConfiguration yaml) throws YamlLoadException;
	public abstract void load(YamlConfiguration yaml);
}
