package org.oddlama.vane.annotation.config

/**
 * Entry for a two-level nested map structure mapping keys to nested material maps.
 *
 * @property key Top-level key in the nested map.
 * @property value Nested map entries under [key].
 */
@Retention(AnnotationRetention.RUNTIME)
// Nested value only: this annotation is written inside [ConfigMaterialMapMapMap]'s `def`, never on
// a
// field. An empty target list is what says so, and it also keeps `ConfigManager`'s
// `Config*` field scan from ever picking it up as a config field of its own.
@Target()
annotation class ConfigMaterialMapMapMapEntry(
    val key: String,
    vararg val value: ConfigMaterialMapMapEntry,
)
