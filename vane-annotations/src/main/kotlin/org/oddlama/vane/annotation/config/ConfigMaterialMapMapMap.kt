package org.oddlama.vane.annotation.config

/**
 * Nested map configuration where the innermost values are Materials.
 *
 * @property def Default nested-map entries.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialMapMapMap(
    val def: Array<ConfigMaterialMapMapMapEntry>,
    val desc: String,
    val metrics: Boolean = false,
)
