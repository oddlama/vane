package org.oddlama.vane.core.data

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType

/**
 * Persistent-data based cooldown helper.
 *
 * @param key persistent data key used to store last-use timestamp.
 * @param cooldownTime cooldown duration in milliseconds.
 */
class CooldownData(private val key: NamespacedKey, private val cooldownTime: Long) {
    /** Returns elapsed milliseconds since last recorded usage. */
    private fun PersistentDataHolder.elapsedSinceLast(): Long =
        System.currentTimeMillis() -
            persistentDataContainer.getOrDefault(key, PersistentDataType.LONG, 0L)

    /**
     * Updates the cooldown data if and only if the cooldown has been exceeded.
     *
     * @return true if cooldownTime has been exceeded.
     */
    fun checkOrUpdateCooldown(holder: PersistentDataHolder): Boolean {
        if (holder.elapsedSinceLast() < cooldownTime) return false
        holder.persistentDataContainer.set(key, PersistentDataType.LONG, System.currentTimeMillis())
        return true
    }

    /** @return the status of the cooldown without updating. */
    fun peekCooldown(holder: PersistentDataHolder): Boolean =
        holder.elapsedSinceLast() >= cooldownTime

    /** Clears stored cooldown timestamp. */
    fun clear(holder: PersistentDataHolder) = holder.persistentDataContainer.remove(key)
}
