package org.oddlama.vane.core.persistent;

import static org.reflections.ReflectionUtils.*;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import org.json.JSONObject;

import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.functional.Function1;

public class PersistentSerializer {
	private static final Map<Class<?>, Function1<Object, String>> serializers = new HashMap<>();
	private static final Map<Class<?>, Function1<String, Object>> deserializers = new HashMap<>();
	static {
		// Primitive types
		serializers.put(boolean.class,    String::valueOf);
		serializers.put(Boolean.class,    String::valueOf);
		serializers.put(char.class,       String::valueOf);
		serializers.put(Character.class,  String::valueOf);
		serializers.put(double.class,     String::valueOf);
		serializers.put(Double.class,     String::valueOf);
		serializers.put(float.class,      String::valueOf);
		serializers.put(Float.class,      String::valueOf);
		serializers.put(int.class,        String::valueOf);
		serializers.put(Integer.class,    String::valueOf);
		serializers.put(long.class,       String::valueOf);
		serializers.put(Long.class,       String::valueOf);

		deserializers.put(boolean.class,    Boolean::parseBoolean);
		deserializers.put(Boolean.class,    Boolean::valueOf);
		deserializers.put(char.class,       s -> s.charAt(0));
		deserializers.put(Character.class,  s -> s == null ? null : s.charAt(0));
		deserializers.put(double.class,     Double::parseDouble);
		deserializers.put(Double.class,     Double::valueOf);
		deserializers.put(float.class,      Float::parseFloat);
		deserializers.put(Float.class,      Float::valueOf);
		deserializers.put(int.class,        Integer::parseInt);
		deserializers.put(Integer.class,    Integer::valueOf);
		deserializers.put(long.class,       Long::parseLong);
		deserializers.put(Long.class,       Long::valueOf);

		// String
		serializers.put(String.class, s -> (String)s);
		deserializers.put(String.class, s -> s);

		// Bukkit types
	}

	public static String to_string(final Class<?> cls, final Object value) {
		final var serializer = serializers.get(cls);
		if (serializer == null) {
			// TODO severe
			System.out.println("no serializer for " + cls);
			return null;
		}
		return serializer.apply(value);
	}

	public static Object to_json(final Field field, final Object value) {
		return to_json(field.getGenericType(), value);
	}

	public static Object to_json(final Type type, final Object value) {
		if (type instanceof ParameterizedType) {
			final var parameterized_type = (ParameterizedType)type;
			final var map_interface = find_parameterized_interface(Map.class, type);
			return null;
			//final var base_type = parameterized_type.getRawType();
			//if (base_type.equals(Map.class)) {
			//	final var type_args = parameterized_type.getActualTypeArguments();
			//	final var K = (Class<?>)type_args[0];
			//	final var V = (Class<?>)type_args[1];
			//	final var map = (Map<?,?>)value;
			//	final var json = new JSONObject();
			//	final var key = new JSONObject();
			//	map.forEach((k, v) -> json.put(to_string(K, k), to_json(V, v)));
			//	return json;
			//} else {
			//	// TODO severe
			//	return null;
			//}
		} else {
			return to_string((Class<?>)type, value);
		}
	}
}
