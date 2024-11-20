package org.oddlama.vane.core.persistent;

import static org.oddlama.vane.util.MaterialUtil.material_from;
import static org.oddlama.vane.util.StorageUtil.namespaced_key;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.oddlama.vane.util.LazyBlock;
import org.oddlama.vane.util.LazyLocation;

public class PersistentSerializer {

    @FunctionalInterface
    public static interface Function<T1, R> {
        R apply(T1 t1) throws IOException;
    }

    private static Object serialize_namespaced_key(@NotNull final Object o) throws IOException {
        return ((NamespacedKey) o).toString();
    }

    private static NamespacedKey deserialize_namespaced_key(@NotNull final Object o) throws IOException {
        final var s = ((String) o).split(":");
        if (s.length != 2) {
            throw new IOException("Invalid namespaced key '" + s + "'");
        }
        return namespaced_key(s[0], s[1]);
    }

    private static Object serialize_lazy_location(@NotNull final Object o) throws IOException {
        final var lazy_location = (LazyLocation) o;
        final var location = lazy_location.location();
        final var json = new JSONObject();
        json.put("world_id", to_json(UUID.class, lazy_location.world_id()));
        json.put("x", to_json(double.class, location.getX()));
        json.put("y", to_json(double.class, location.getY()));
        json.put("z", to_json(double.class, location.getZ()));
        json.put("pitch", to_json(float.class, location.getPitch()));
        json.put("yaw", to_json(float.class, location.getYaw()));
        return json;
    }

    private static LazyLocation deserialize_lazy_location(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var world_id = from_json(UUID.class, json.get("world_id"));
        final var x = from_json(double.class, json.get("x"));
        final var y = from_json(double.class, json.get("y"));
        final var z = from_json(double.class, json.get("z"));
        final var pitch = from_json(float.class, json.get("pitch"));
        final var yaw = from_json(float.class, json.get("yaw"));
        return new LazyLocation(world_id, x, y, z, yaw, pitch);
    }

    private static Object serialize_lazy_block(@NotNull final Object o) throws IOException {
        final var lazy_block = (LazyBlock) o;
        final var json = new JSONObject();
        json.put("world_id", to_json(UUID.class, lazy_block.world_id()));
        json.put("x", to_json(int.class, lazy_block.x()));
        json.put("y", to_json(int.class, lazy_block.y()));
        json.put("z", to_json(int.class, lazy_block.z()));
        return json;
    }

    private static LazyBlock deserialize_lazy_block(@NotNull final Object o) throws IOException {
        final var json = (JSONObject) o;
        final var world_id = from_json(UUID.class, json.get("world_id"));
        final var x = from_json(int.class, json.get("x"));
        final var y = from_json(int.class, json.get("y"));
        final var z = from_json(int.class, json.get("z"));
        return new LazyBlock(world_id, x, y, z);
    }

    private static Object serialize_material(@NotNull final Object o) throws IOException {
        return to_json(NamespacedKey.class, ((Material) o).getKey());
    }

    private static Material deserialize_material(@NotNull final Object o) throws IOException {
        return material_from(from_json(NamespacedKey.class, o));
    }

    private static Object serialize_item_stack(@NotNull final Object o) throws IOException {
        return new String(Base64.getEncoder().encode(((ItemStack) o).serializeAsBytes()), StandardCharsets.UTF_8);
    }

    private static ItemStack deserialize_item_stack(@NotNull final Object o) throws IOException {
        return ItemStack.deserializeBytes(Base64.getDecoder().decode(((String) o).getBytes(StandardCharsets.UTF_8)));
    }

    private static boolean is_null(Object o) {
        return o == null || o == JSONObject.NULL;
    }

    public static final Map<Class<?>, Function<Object, Object>> serializers = new HashMap<>();
    public static final Map<Class<?>, Function<Object, Object>> deserializers = new HashMap<>();

    static {
        // Primitive types
        serializers.put(boolean.class, String::valueOf);
        serializers.put(char.class, String::valueOf);
        serializers.put(double.class, String::valueOf);
        serializers.put(float.class, String::valueOf);
        serializers.put(int.class, String::valueOf);
        serializers.put(long.class, String::valueOf);
        serializers.put(Boolean.class, String::valueOf);
        serializers.put(Character.class, String::valueOf);
        serializers.put(Double.class, String::valueOf);
        serializers.put(Float.class, String::valueOf);
        serializers.put(Integer.class, String::valueOf);
        serializers.put(Long.class, String::valueOf);

        deserializers.put(boolean.class, x -> Boolean.parseBoolean((String) x));
        deserializers.put(char.class, x -> ((String) x).charAt(0));
        deserializers.put(double.class, x -> Double.parseDouble((String) x));
        deserializers.put(float.class, x -> Float.parseFloat((String) x));
        deserializers.put(int.class, x -> Integer.parseInt((String) x));
        deserializers.put(long.class, x -> Long.parseLong((String) x));
        deserializers.put(Boolean.class, x -> Boolean.valueOf((String) x));
        deserializers.put(Character.class, x -> ((String) x).charAt(0));
        deserializers.put(Double.class, x -> Double.valueOf((String) x));
        deserializers.put(Float.class, x -> Float.valueOf((String) x));
        deserializers.put(Integer.class, x -> Integer.valueOf((String) x));
        deserializers.put(Long.class, x -> Long.valueOf((String) x));

        // Other types
        serializers.put(String.class, x -> x);
        deserializers.put(String.class, x -> x);
        serializers.put(UUID.class, Object::toString);
        deserializers.put(UUID.class, x -> UUID.fromString((String) x));

        // Bukkit types
        serializers.put(NamespacedKey.class, PersistentSerializer::serialize_namespaced_key);
        deserializers.put(NamespacedKey.class, PersistentSerializer::deserialize_namespaced_key);
        serializers.put(LazyLocation.class, PersistentSerializer::serialize_lazy_location);
        deserializers.put(LazyLocation.class, PersistentSerializer::deserialize_lazy_location);
        serializers.put(LazyBlock.class, PersistentSerializer::serialize_lazy_block);
        deserializers.put(LazyBlock.class, PersistentSerializer::deserialize_lazy_block);
        serializers.put(Material.class, PersistentSerializer::serialize_material);
        deserializers.put(Material.class, PersistentSerializer::deserialize_material);
        serializers.put(ItemStack.class, PersistentSerializer::serialize_item_stack);
        deserializers.put(ItemStack.class, PersistentSerializer::deserialize_item_stack);
    }

    public static Object to_json(final Field field, final Object value) throws IOException {
        return to_json(field.getGenericType(), value);
    }

    public static Object to_json(final Class<?> cls, final Object value) throws IOException {
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
            final var parameterized_type = (ParameterizedType) type;
            final var base_type = parameterized_type.getRawType();
            final var type_args = parameterized_type.getActualTypeArguments();
            if (base_type.equals(Map.class)) {
                final var K = (Class<?>) type_args[0];
                final var V = type_args[1];
                final var json = new JSONObject();
                for (final var e : ((Map<?, ?>) value).entrySet()) {
                    json.put((String) to_json(K, e.getKey()), to_json(V, e.getValue()));
                }
                return json;
            } else if (base_type.equals(Set.class)) {
                final var T = type_args[0];
                final var json = new JSONArray();
                for (final var t : (Set<?>) value) {
                    json.put(to_json(T, t));
                }
                return json;
            } else if (base_type.equals(List.class)) {
                final var T = type_args[0];
                final var json = new JSONArray();
                for (final var t : (List<?>) value) {
                    json.put(to_json(T, t));
                }
                return json;
            } else {
                throw new IOException("Cannot serialize " + type + ". This is a bug.");
            }
        } else {
            return to_json((Class<?>) type, value);
        }
    }

    public static Object from_json(final Field field, final Object value) throws IOException {
        return from_json(field.getGenericType(), value);
    }

    @SuppressWarnings("unchecked")
    public static <U> U from_json(final Class<U> cls, final Object value) throws IOException {
        final var deserializer = deserializers.get(cls);
        if (deserializer == null) {
            throw new IOException("Cannot deserialize " + cls + ". This is a bug.");
        }
        if (is_null(value)) {
            return null;
        }
        return (U) deserializer.apply(value);
    }

    public static Object from_json(final Type type, final Object json) throws IOException {
        if (type instanceof ParameterizedType) {
            final var parameterized_type = (ParameterizedType) type;
            final var base_type = parameterized_type.getRawType();
            final var type_args = parameterized_type.getActualTypeArguments();
            if (base_type.equals(Map.class)) {
                final var K = (Class<?>) type_args[0];
                final var V = type_args[1];
                final var value = new HashMap<Object, Object>();
                for (final var key : ((JSONObject) json).keySet()) {
                    value.put(from_json(K, key), from_json(V, ((JSONObject) json).get(key)));
                }
                return value;
            } else if (base_type.equals(Set.class)) {
                final var T = type_args[0];
                final var value = new HashSet<Object>();
                for (final var t : (JSONArray) json) {
                    value.add(from_json(T, t));
                }
                return value;
            } else if (base_type.equals(List.class)) {
                final var T = type_args[0];
                final var value = new ArrayList<Object>();
                for (final var t : (JSONArray) json) {
                    value.add(from_json(T, t));
                }
                return value;
            } else {
                throw new IOException("Cannot deserialize " + type + ". This is a bug.");
            }
        } else {
            return from_json((Class<?>) type, json);
        }
    }
}
