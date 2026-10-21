package org.oddlama.vane.util

import java.util.*
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

/** Persistent-data and namespaced-key storage helpers. */
object StorageUtil {
    /** Creates a namespaced key from namespace and value. */
    @JvmStatic
    fun namespacedKey(namespace: String, key: String): NamespacedKey = NamespacedKey(namespace, key)

    /** Creates a sub-key under an existing namespaced key. */
    fun subkey(key: NamespacedKey, sub: String?): NamespacedKey =
        namespacedKey(key.namespace(), "${key.value()}.$sub")

    /** Returns whether a serialized location exists at the given key. */
    fun storageHasLocation(data: PersistentDataContainer, key: NamespacedKey): Boolean =
        data.has(subkey(key, "world"), PersistentDataType.STRING)

    /** Reads a serialized location from persistent data. */
    @JvmStatic
    fun storageGetLocation(
        data: PersistentDataContainer,
        key: NamespacedKey,
        def: Location?,
    ): Location? {
        return try {
            val worldId = data.get(subkey(key, "world"), PersistentDataType.STRING) ?: return def
            val x = data.get(subkey(key, "x"), PersistentDataType.DOUBLE) ?: return def
            val y = data.get(subkey(key, "y"), PersistentDataType.DOUBLE) ?: return def
            val z = data.get(subkey(key, "z"), PersistentDataType.DOUBLE) ?: return def
            val yaw = data.get(subkey(key, "yaw"), PersistentDataType.FLOAT) ?: return def
            val pitch = data.get(subkey(key, "pitch"), PersistentDataType.FLOAT) ?: return def
            val world = Bukkit.getWorld(UUID.fromString(worldId)) ?: return def
            Location(world, x, y, z, yaw, pitch)
        } catch (_: IllegalArgumentException) {
            def
        }
    }

    /** Removes a serialized location from persistent data. */
    @JvmStatic
    fun storageRemoveLocation(data: PersistentDataContainer, key: NamespacedKey) {
        listOf("world", "x", "y", "z", "yaw", "pitch").forEach { data.remove(subkey(key, it)) }
    }

    /** Stores a location into persistent data. */
    @JvmStatic
    fun storageSetLocation(data: PersistentDataContainer, key: NamespacedKey, location: Location) {
        data.set(subkey(key, "world"), PersistentDataType.STRING, location.world.uid.toString())
        data.set(subkey(key, "x"), PersistentDataType.DOUBLE, location.x)
        data.set(subkey(key, "y"), PersistentDataType.DOUBLE, location.y)
        data.set(subkey(key, "z"), PersistentDataType.DOUBLE, location.z)
        data.set(subkey(key, "yaw"), PersistentDataType.FLOAT, location.yaw)
        data.set(subkey(key, "pitch"), PersistentDataType.FLOAT, location.pitch)
    }
}
