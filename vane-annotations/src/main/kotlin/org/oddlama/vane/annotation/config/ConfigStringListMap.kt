package org.oddlama.vane.annotation.config

/**
 * Map from string keys to lists of strings configuration entries.
 *
 * @property def Default map entries.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigStringListMap(
    val def: Array<ConfigStringListMapEntry>,
    val desc: String,
    val metrics: Boolean = false,
)
