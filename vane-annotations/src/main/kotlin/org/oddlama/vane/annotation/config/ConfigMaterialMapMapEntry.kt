package org.oddlama.vane.annotation.config

/**
 * Entry for a map from string keys to maps of materials (one nesting level).
 *
 * @property key Top-level key in the nested map.
 * @property value Nested material map entries under [key].
 */
@Retention(AnnotationRetention.RUNTIME)
// Nested value only: this annotation is written inside [ConfigMaterialMapMapMapEntry]'s `def`,
// never on a
// field. An empty target list is what says so, and it also keeps `ConfigManager`'s
// `Config*` field scan from ever picking it up as a config field of its own.
@Target()
annotation class ConfigMaterialMapMapEntry(
    val key: String,
    vararg val value: ConfigMaterialMapEntry,
)
