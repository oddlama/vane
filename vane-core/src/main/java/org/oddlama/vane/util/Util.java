package org.oddlama.vane.util;

import java.util.Arrays;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.annotation.Annotation;

import org.bukkit.NamespacedKey;

public class Util {
	@SuppressWarnings("deprecation")
	public static NamespacedKey namespaced_key(String namespace, String key) {
		return new NamespacedKey(namespace, key);
	}

	public static long ms_to_ticks(long ms) {
		return ms / 50;
	}

	public static long ticks_to_ms(long ticks) {
		return ticks * 50;
	}

	public static <T> T[] prepend(T[] arr, T element) {
		final var n = arr.length;
		arr = Arrays.copyOf(arr, n + 1);
		for (int i = arr.length - 1; i > 0; --i) {
			arr[i] = arr[i - 1];
		}
		arr[0] = element;
		return arr;
	}

	public static <T> T[] append(T[] arr, T element) {
		final var n = arr.length;
		arr = Arrays.copyOf(arr, n + 1);
		arr[n] = element;
		return arr;
	}

	/**
	 * Changes the annotation value for the given key of the given annotation to new_value and returns
	 * the previous value.
	 */
	@SuppressWarnings("unchecked")
	public static Object change_annotation_value(Annotation annotation, String key, Object new_value) {
		Object handler = Proxy.getInvocationHandler(annotation);
		Field f;
		try {
			f = handler.getClass().getDeclaredField("memberValues");
		} catch (NoSuchFieldException | SecurityException e) {
			throw new IllegalStateException(e);
		}
		f.setAccessible(true);
		Map<String, Object> member_values;
		try {
			member_values = (Map<String, Object>)f.get(handler);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
		var old_value = member_values.get(key);
		if (old_value == null || old_value.getClass() != new_value.getClass()) {
			throw new IllegalArgumentException();
		}
		member_values.put(key, new_value);
		return old_value;
	}
}
