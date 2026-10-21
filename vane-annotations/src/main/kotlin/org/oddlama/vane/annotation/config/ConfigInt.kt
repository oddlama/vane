package org.oddlama.vane.annotation.config

/**
 * Integer configuration field with optional bounds.
 *
 * @property def Default value.
 * @property min Minimum accepted value.
 * @property max Maximum accepted value.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigInt(
    val def: Int,
    val min: Int = Int.MIN_VALUE,
    val max: Int = Int.MAX_VALUE,
    val desc: String,
    val metrics: Boolean = true,
)
