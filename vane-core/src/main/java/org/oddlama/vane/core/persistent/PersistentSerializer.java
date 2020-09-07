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
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.json.JSONObject;
import org.json.JSONArray;

import org.jetbrains.annotations.NotNull;

public class PersistentSerializer {
	@FunctionalInterface
	public static interface Function<T1, R> {
		R apply(T1 t1) throws IOException;
	}

	private static Object serialize_location(@NotNull final Object o) throws IOException {
		final var location = (Location)o;
		final var json = new JSONObject();
		json.put("world_id", primitive_to_json(UUID.class,   location.getWorld().getUID()));
		json.put("x",        primitive_to_json(double.class, location.getX()));
		json.put("y",        primitive_to_json(double.class, location.getY()));
		json.put("z",        primitive_to_json(double.class, location.getZ()));
		json.put("pitch",    primitive_to_json(float.class, location.getPitch()));
		json.put("yaw",      primitive_to_json(float.class, location.getYaw()));
		return json;
	}

	private static Location deserialize_location(@NotNull final Object o) throws IOException {
		final var json = (JSONObject)o;
		final var world_id = primitive_from_json(UUID.class,   json.get("world_id"));
		final var x        = primitive_from_json(double.class, json.get("x"));
		final var y        = primitive_from_json(double.class, json.get("y"));
		final var z        = primitive_from_json(double.class, json.get("z"));
		final var pitch    = primitive_from_json(float.class, json.get("pitch"));
		final var yaw      = primitive_from_json(float.class, json.get("yaw"));
		return new Location(Bukkit.getWorld(world_id), x, y, z, yaw, pitch);
	}

	private static boolean is_null(Object o) {
		return o == null || o == JSONObject.NULL;
	}

	private static final Map<Class<?>, Function<Object, Object>> serializers = new HashMap<>();
	private static final Map<Class<?>, Function<Object, Object>> deserializers = new HashMap<>();
	static {
		// Primitive types
		serializers.put(boolean.class,    String::valueOf);
		serializers.put(char.class,       String::valueOf);
		serializers.put(double.class,     String::valueOf);
		serializers.put(float.class,      String::valueOf);
		serializers.put(int.class,        String::valueOf);
		serializers.put(long.class,       String::valueOf);
		serializers.put(Boolean.class,    String::valueOf);
		serializers.put(Character.class,  String::valueOf);
		serializers.put(Double.class,     String::valueOf);
		serializers.put(Float.class,      String::valueOf);
		serializers.put(Integer.class,    String::valueOf);
		serializers.put(Long.class,       String::valueOf);

		deserializers.put(boolean.class,    x -> Boolean.parseBoolean((String)x));
		deserializers.put(char.class,       x -> ((String)x).charAt(0));
		deserializers.put(double.class,     x -> Double.parseDouble((String)x));
		deserializers.put(float.class,      x -> Float.parseFloat((String)x));
		deserializers.put(int.class,        x -> Integer.parseInt((String)x));
		deserializers.put(long.class,       x -> Long.parseLong((String)x));
		deserializers.put(Boolean.class,    x -> Boolean.valueOf((String)x));
		deserializers.put(Character.class,  x -> ((String)x).charAt(0));
		deserializers.put(Double.class,     x -> Double.valueOf((String)x));
		deserializers.put(Float.class,      x -> Float.valueOf((String)x));
		deserializers.put(Integer.class,    x -> Integer.valueOf((String)x));
		deserializers.put(Long.class,       x -> Long.valueOf((String)x));

		// Other types
		serializers.put(String.class,   x -> x);
		deserializers.put(String.class, x -> x);
		serializers.put(UUID.class,     x -> ((UUID)x).toString());
		deserializers.put(UUID.class,   x -> UUID.fromString((String)x));

		// Bukkit types
		serializers.put(Location.class,   PersistentSerializer::serialize_location);
		deserializers.put(Location.class, PersistentSerializer::deserialize_location);
	}

	public static Object to_json(final Field field, final Object value) throws IOException {
		return to_json(field.getGenericType(), value);
	}

	public static Object primitive_to_json(final Class<?> cls, final Object value) throws IOException {
		final var serializer = serializers.get(cls);
		if (serializer == null) {
			throw new IOException("Cannot serialize " + cls + ". This is a bug.");
		}
		if (is_null(value)) {
			return JSONObject.NULL;
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
					json.put((String)primitive_to_json(K, e.getKey()), to_json(V, e.getValue()));
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
			return primitive_to_json((Class<?>)type, value);
		}
	}

	public static Object from_json(final Field field, final Object value) throws IOException {
		return from_json(field.getGenericType(), value);
	}

	@SuppressWarnings("unchecked")
	public static<U> U primitive_from_json(final Class<U> cls, final Object value) throws IOException {
		final var deserializer = deserializers.get(cls);
		if (deserializer == null) {
			throw new IOException("Cannot deserialize " + cls + ". This is a bug.");
		}
		if (is_null(value)) {
			return null;
		}
		return (U)deserializer.apply(value);
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
					value.put(primitive_from_json(K, key), from_json(V, ((JSONObject)json).get(key)));
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
			return primitive_from_json((Class<?>)type, json);
		}
	}
}
