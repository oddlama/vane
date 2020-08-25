package org.oddlama.vane.core.persistent;

import static org.reflections.ReflectionUtils.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Function;

public class PersistentField {
	private Object owner;
	private Field field;
	private String path;

	public PersistentField(Object owner, Field field, Function<String, String> map_name) {
		this.owner = owner;
		this.field = field;
		this.path = map_name.apply(field.getName().substring("storage_".length()));

		field.setAccessible(true);
	}

	public String path() {
		return path;
	}

	public Object get() {
		try {
			return field.get(owner);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}

	public void save(Map<String, Object> map) {
		map.put(path, get());
	}

	public void load(Map<String, Object> map) throws LoadException {
		if (!map.containsKey(path)) {
			throw new LoadException("Missing key in persistent map: '" + path + "'");
		}

		try {
			field.set(owner, map.get(path));
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Invalid field access on '" + field.getName() + "'. This is a bug.");
		}
	}
}
