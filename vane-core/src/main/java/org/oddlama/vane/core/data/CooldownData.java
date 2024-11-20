package org.oddlama.vane.core.data;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class CooldownData {

    private final NamespacedKey key;
    private final long cooldown_time;

    public CooldownData(NamespacedKey key, long cooldown_time) {
        this.key = key;
        this.cooldown_time = cooldown_time;
    }

    /**
     * Updates the cooldown data if and only if the cooldown has been exceeded.
     *
     * @return returns true if cooldown_time has been exceeded.
     */
    public boolean check_or_update_cooldown(final PersistentDataHolder holder) {
        final var persistent_data = holder.getPersistentDataContainer();
        final var last_time = persistent_data.getOrDefault(key, PersistentDataType.LONG, 0L);
        final var now = System.currentTimeMillis();
        if (now - last_time < cooldown_time) {
            return false;
        }

        persistent_data.set(key, PersistentDataType.LONG, now);
        return true;
    }

    /**
     * @return Gets the status of the cooldown without updating
     */
    public boolean peek_cooldown(PersistentDataHolder holder) {
        PersistentDataContainer persistent_data = holder.getPersistentDataContainer();
        Long last_time = persistent_data.getOrDefault(this.key, PersistentDataType.LONG, 0L);
        long now = System.currentTimeMillis();
        return now - last_time >= this.cooldown_time;
    }

    public void clear(final PersistentDataHolder holder) {
        final var persistent_data = holder.getPersistentDataContainer();
        persistent_data.remove(key);
    }
}
