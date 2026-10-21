package org.oddlama.vane.annotation.config

import org.bukkit.Material

/**
 * Entry mapping a string key to a single Material.
 *
 * @property key Top-level key in the map.
 * @property value Material mapped to [key].
 */
@Retention(AnnotationRetention.RUNTIME)
// Nested value only: this annotation is written inside [ConfigMaterialMapMapEntry]'s `def`, never
// on a
// field. An empty target list is what says so, and it also keeps `ConfigManager`'s
// `Config*` field scan from ever picking it up as a config field of its own.
@Target()
annotation class ConfigMaterialMapEntry(val key: String, val value: Material)
