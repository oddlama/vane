package org.oddlama.vane.core.item;

import java.util.HashMap;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

public class CustomModelDataRegistry implements org.oddlama.vane.core.item.api.CustomModelDataRegistry {

    private final HashMap<NamespacedKey, Range> reserved_ranges = new HashMap<>();

    @Override
    public boolean has(int data) {
        return reserved_ranges.values().stream().anyMatch(r -> r.contains(data));
    }

    @Override
    public boolean hasAny(Range range) {
        return reserved_ranges.values().stream().anyMatch(r -> r.overlaps(range));
    }

    @Override
    public @Nullable Range get(NamespacedKey resourceKey) {
        return reserved_ranges.get(resourceKey);
    }

    @Override
    public @Nullable NamespacedKey get(int data) {
        for (final var kv : reserved_ranges.entrySet()) {
            if (kv.getValue().contains(data)) {
                return kv.getKey();
            }
        }

        return null;
    }

    @Override
    public @Nullable NamespacedKey get(Range range) {
        for (final var kv : reserved_ranges.entrySet()) {
            if (kv.getValue().overlaps(range)) {
                return kv.getKey();
            }
        }

        return null;
    }

    @Override
    public void reserve(NamespacedKey resourceKey, Range range) {
        final var existing = get(range);
        if (existing != null) {
            throw new IllegalArgumentException("Cannot reserve range " + range + ", already registered by " + existing);
        }
        reserved_ranges.put(resourceKey, range);
    }

    @Override
    public void reserveSingle(NamespacedKey resourceKey, int data) {
        final var existing = get(data);
        if (existing != null) {
            throw new IllegalArgumentException(
                "Cannot reserve customModelData " + data + ", already registered by " + existing
            );
        }
        reserved_ranges.put(resourceKey, new Range(data, data + 1));
    }
}
