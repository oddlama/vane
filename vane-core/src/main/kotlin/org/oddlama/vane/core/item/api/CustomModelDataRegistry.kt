package org.oddlama.vane.core.item.api

import org.bukkit.NamespacedKey

/** Contract for reserving and resolving custom model data ids. */
interface CustomModelDataRegistry {
    /** A range of custom model data ids. */
    @JvmRecord
    data class Range(val from: Int, val to: Int) {
        /** Returns whether the range contains the given data id. */
        fun contains(data: Int): Boolean = data in from..<to

        /** Returns whether this range overlaps another range. */
        fun overlaps(range: Range): Boolean = to > range.from && from < range.to

        init {
            require(from < to) { "A range must contain at least one integer" }
            require(from > -(1 shl 24)) {
                "A range cannot contain a number <= -2^24, as these cannot be accurately represented in JSON."
            }
            require(to < (1 shl 24)) {
                "A range cannot contain a number >= 2^24, as these cannot be accurately represented in JSON."
            }
        }
    }

    /** Returns true if the given custom model data is already reserved. */
    fun has(data: Int): Boolean

    /** Returns true if any custom model data in the given range is already reserved. */
    fun hasAny(range: Range): Boolean

    /** Returns the range associated to a specific key. */
    fun get(resourceKey: NamespacedKey): Range?

    /** Returns the key associated to specific custom model data, if any. */
    fun get(data: Int): NamespacedKey?

    /** Returns the key associated to the first encountered registered id in the given range. */
    fun get(range: Range): NamespacedKey?

    /** Reserves the given range. */
    fun reserve(resourceKey: NamespacedKey, range: Range)

    /** Reserves a single id. */
    fun reserveSingle(resourceKey: NamespacedKey, data: Int)
}
