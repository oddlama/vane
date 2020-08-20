package org.oddlama.vane.core.config;

import static org.reflections.ReflectionUtils.*;

import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import org.bukkit.configuration.file.YamlConfiguration;

import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.core.YamlLoadException;

public class ConfigBooleanField extends ConfigField<Boolean> {
	private ConfigBoolean annotation;
	private String desc;

	public ConfigBooleanField(Object owner, Field field, Function<String, String> map_name, ConfigBoolean annotation) {
		super(owner, field, map_name, "boolean");
		this.annotation = annotation;
		this.desc = annotation.desc();
		if (desc.equals("")) {
			try {
				desc = (String)owner.getClass().getMethod(field.getName() + "_desc").invoke(owner);
			} catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
				throw new RuntimeException("Could not call " + owner.getClass().getName() + "." + field.getName() + "_desc() to override description value", e);
			}
		}
	}

	@Override
	public void generate_yaml(StringBuilder builder) {
		append_description(builder, desc);
		append_default_value(builder, annotation.def());
		append_field_definition(builder, annotation.def());
	}

	@Override
	public void check_loadable(YamlConfiguration yaml) throws YamlLoadException {
		check_yaml_path(yaml);

		if (!yaml.isBoolean(get_yaml_path())) {
			throw new YamlLoadException("Invalid type for yaml path '" + get_yaml_path() + "', expected boolean");
		}
	}

	public void load(YamlConfiguration yaml) {
		try {
			field.setBoolean(owner, yaml.getBoolean(get_yaml_path()));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}

