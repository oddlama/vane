package org.oddlama.vane.core.item

import org.bukkit.NamespacedKey
import org.oddlama.vane.core.item.api.CustomModelDataRegistry

/** Registry that tracks reserved custom model data ranges. */
class CustomModelDataRegistry : CustomModelDataRegistry {
    /** Reserved ranges keyed by item key. */
    private val reservedRanges = mutableMapOf<NamespacedKey, CustomModelDataRegistry.Range>()

    /** Returns whether a model data value is reserved. */
    override fun has(data: Int): Boolean = reservedRanges.values.any { it.contains(data) }

    /** Returns whether any reserved range overlaps the given range. */
    override fun hasAny(range: CustomModelDataRegistry.Range): Boolean =
        reservedRanges.values.any { it.overlaps(range) }

    /** Returns the reserved range for an item key. */
    override fun get(resourceKey: NamespacedKey): CustomModelDataRegistry.Range? =
        reservedRanges[resourceKey]

    /** Returns the item key owning a model data value. */
    override fun get(data: Int): NamespacedKey? =
        reservedRanges.entries.firstOrNull { it.value.contains(data) }?.key

    /** Returns the item key owning an overlapping range. */
    override fun get(range: CustomModelDataRegistry.Range): NamespacedKey? =
        reservedRanges.entries.firstOrNull { it.value.overlaps(range) }?.key

    /** Reserves a model data range for an item key. */
    override fun reserve(resourceKey: NamespacedKey, range: CustomModelDataRegistry.Range) {
        val existing = get(range)
        require(existing == null) { "Cannot reserve range $range, already registered by $existing" }
        reservedRanges[resourceKey] = range
    }

    /** Reserves a single model data value for an item key. */
    override fun reserveSingle(resourceKey: NamespacedKey, data: Int) {
        val existing = get(data)
        require(existing == null) {
            "Cannot reserve customModelData $data, already registered by $existing"
        }
        reservedRanges[resourceKey] = CustomModelDataRegistry.Range(data, data + 1)
    }
}
