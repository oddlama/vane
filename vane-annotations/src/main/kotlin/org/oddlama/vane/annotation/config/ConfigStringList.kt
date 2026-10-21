package org.oddlama.vane.annotation.config

/**
 * List of strings configuration field.
 *
 * @property def Default list values.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigStringList(
    val def: Array<String>,
    val desc: String,
    val metrics: Boolean = false,
)
