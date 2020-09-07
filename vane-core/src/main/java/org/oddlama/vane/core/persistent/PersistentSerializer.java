package org.oddlama.vane.core.persistent;

import static org.reflections.ReflectionUtils.*;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Field;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.function.Function;
import org.json.JSONObject;
import org.json.JSONArray;

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

		// Other types
		serializers.put(String.class, s -> (String)s);
		deserializers.put(String.class, s -> s);
		serializers.put(UUID.class, u -> u.toString());
		deserializers.put(UUID.class, UUID::fromString);

		// Bukkit types
	}

	public static Object to_json(final Field field, final Object value) throws IOException {
		return to_json(field.getGenericType(), value);
	}

	public static String to_string(final Class<?> cls, final Object value) throws IOException {
		final var serializer = serializers.get(cls);
		if (serializer == null) {
			throw new IOException("Cannot serialize " + cls + ". This is a bug.");
		}
		return serializer.apply(value);
	}

	public static Object to_json(final Type type, final Object value) throws IOException {
		if (type instanceof ParameterizedType) {
			final var parameterized_type = (ParameterizedType)type;
			final var base_type = parameterized_type.getRawType();
			final var type_args = parameterized_type.getActualTypeArguments();
			if (base_type.equals(Map.class)) {
				final var K = (Class<?>)type_args[0];
				final var V = type_args[1];
				final var json = new JSONObject();
				for (final var e : ((Map<?,?>)value).entrySet()) {
					json.put(to_string(K, e.getKey()), to_json(V, e.getValue()));
				}
				return json;
			} else if (base_type.equals(Set.class)) {
				final var T = type_args[0];
				final var json = new JSONArray();
				for (final var t : (Set<?>)value) {
					json.put(to_json(T, t));
				}
				return json;
			} else if (base_type.equals(List.class)) {
				final var T = type_args[0];
				final var json = new JSONArray();
				for (final var t : (List<?>)value) {
					json.put(to_json(T, t));
				}
				return json;
			} else {
				throw new IOException("Cannot serialize " + type + ". This is a bug.");
			}
		} else {
			return to_string((Class<?>)type, value);
		}
	}

	public static Object from_json(final Field field, final Object value) throws IOException {
		return from_json(field.getGenericType(), value);
	}

	public static Object from_string(final Class<?> cls, final String value) throws IOException {
		final var deserializer = deserializers.get(cls);
		if (deserializer == null) {
			throw new IOException("Cannot deserialize " + cls + ". This is a bug.");
		}
		return deserializer.apply(value);
	}

	public static Object from_json(final Type type, final Object json) throws IOException {
		if (type instanceof ParameterizedType) {
			final var parameterized_type = (ParameterizedType)type;
			final var base_type = parameterized_type.getRawType();
			final var type_args = parameterized_type.getActualTypeArguments();
			if (base_type.equals(Map.class)) {
				final var K = (Class<?>)type_args[0];
				final var V = type_args[1];
				final var value = new HashMap<Object, Object>();
				for (final var key : ((JSONObject)json).keySet()) {
					value.put(from_string(K, key), from_json(V, ((JSONObject)json).get(key)));
				}
				return value;
			} else if (base_type.equals(Set.class)) {
				final var T = type_args[0];
				final var value = new HashSet<Object>();
				for (final var t : (JSONArray)json) {
					value.add(from_json(T, t));
				}
				return value;
			} else if (base_type.equals(List.class)) {
				final var T = type_args[0];
				final var value = new ArrayList<Object>();
				for (final var t : (JSONArray)json) {
					value.add(from_json(T, t));
				}
				return value;
			} else {
				throw new IOException("Cannot deserialize " + type + ". This is a bug.");
			}
		} else {
			return from_string((Class<?>)type, (String)json);
		}
	}
}
