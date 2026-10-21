package org.oddlama.vane.annotation.item

import org.bukkit.Material

/**
 * Declares that the annotated class represents a custom item used by the module.
 *
 * @property name Identifier or display name for the item.
 * @property base Base material used for the item (Bukkit Material).
 * @property modelData Custom model data value.
 * @property version Item definition version for compatibility checks.
 * @property durability Default durability value for this item.
 * @property enabled Whether the item is enabled by default.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class VaneItem(
    val name: String,
    val base: Material,
    val modelData: Int,
    val version: Int,
    val durability: Int = 0,
    val enabled: Boolean = true,
)
