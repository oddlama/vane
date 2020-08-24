package org.oddlama.vane.core.persistent;

import static org.reflections.ReflectionUtils.*;

import java.lang.Comparable;
import java.lang.StringBuilder;
import java.lang.reflect.Field;
import java.util.function.Function;

import org.apache.commons.lang.WordUtils;

public class PersistentField {
	private Object owner;
	private Field field;
	private String path;

	public PersistentField(Object owner, Field field, Function<String, String> map_name) {
		this.owner = owner;
		this.field = field;
		this.path = map_name.apply(field.getName());

		field.setAccessible(true);
	}

	public String path() {
		return path;
	}

	public void load() throws LoadException {
		throw new LoadException("");
	}
}
