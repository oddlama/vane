package org.oddlama.vane.util;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class StorageUtil {

	@SuppressWarnings("deprecation")
	public static NamespacedKey namespaced_key(String namespace, String key) {
		return new NamespacedKey(namespace, key);
	}

	public static NamespacedKey subkey(NamespacedKey key, String sub) {
		return namespaced_key(key.namespace(), key.value() + "." + sub);
	}

	public static boolean storage_has_location(@NotNull PersistentDataContainer data, NamespacedKey key) {
		return data.has(subkey(key, "world"), PersistentDataType.STRING);
	}

	public static Location storage_get_location(@NotNull PersistentDataContainer data, NamespacedKey key, Location def) {
		try {
			final var world_id = data.get(subkey(key, "world"), PersistentDataType.STRING);
			final var x = data.get(subkey(key, "x"), PersistentDataType.DOUBLE);
			final var y = data.get(subkey(key, "y"), PersistentDataType.DOUBLE);
			final var z = data.get(subkey(key, "z"), PersistentDataType.DOUBLE);
			final var yaw = data.get(subkey(key, "yaw"), PersistentDataType.FLOAT);
			final var pitch = data.get(subkey(key, "pitch"), PersistentDataType.FLOAT);
			final var world = Bukkit.getWorld(UUID.fromString(world_id));
			if (world == null) {
				return def;
			}
			return new Location(world, x, y, z, yaw, pitch);
		} catch (IllegalArgumentException | NullPointerException e) {
			return def;
		}
	}

	public static void storage_remove_location(@NotNull PersistentDataContainer data, NamespacedKey key) {
		data.remove(subkey(key, "world"));
		data.remove(subkey(key, "x"));
		data.remove(subkey(key, "y"));
		data.remove(subkey(key, "z"));
		data.remove(subkey(key, "yaw"));
		data.remove(subkey(key, "pitch"));
	}

	public static void storage_set_location(@NotNull PersistentDataContainer data, NamespacedKey key, @NotNull Location location) {
		data.set(subkey(key, "world"), PersistentDataType.STRING, location.getWorld().getUID().toString());
		data.set(subkey(key, "x"), PersistentDataType.DOUBLE, location.getX());
		data.set(subkey(key, "y"), PersistentDataType.DOUBLE, location.getY());
		data.set(subkey(key, "z"), PersistentDataType.DOUBLE, location.getZ());
		data.set(subkey(key, "yaw"), PersistentDataType.FLOAT, location.getYaw());
		data.set(subkey(key, "pitch"), PersistentDataType.FLOAT, location.getPitch());
	}

}
