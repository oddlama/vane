package org.oddlama.vane.core.item.api;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

public interface CustomModelDataRegistry {
    /** A range of custom model data ids. */
    public static record Range(int from, int to) {
        public Range {
            if (from >= to) {
                throw new IllegalArgumentException("A range must contain at least one integer");
            }
            if (from <= -(1 << 24)) {
                throw new IllegalArgumentException(
                    "A range cannot contain a number <= -2^24, as these cannot be accurately represented in JSON."
                );
            }
            if (to >= (1 << 24)) {
                throw new IllegalArgumentException(
                    "A range cannot contain a number >= 2^24, as these cannot be accurately represented in JSON."
                );
            }
        }

        public boolean contains(int data) {
            return data >= from && data < to;
        }

        public boolean overlaps(Range range) {
            return !(to >= range.from || from >= range.to);
        }
    }

    /** Returns true if the given custom model data is already reserved. */
    public boolean has(int data);

    /** Returns true if any custom model data in the given range is already reserved. */
    public boolean hasAny(Range range);

    /** Returns the range associated to a specific key. */
    public Range get(NamespacedKey resourceKey);

    /** Returns the key associated to specific custom model data, if any. */
    public @Nullable NamespacedKey get(int data);

    /** Returns the key associated to the first encountered registered id in the given range. */
    public @Nullable NamespacedKey get(Range range);

    /** Reserves the given range. */
    public void reserve(NamespacedKey resourceKey, Range range);

    /** Reserves the given range. */
    public void reserveSingle(NamespacedKey resourceKey, int data);
}
