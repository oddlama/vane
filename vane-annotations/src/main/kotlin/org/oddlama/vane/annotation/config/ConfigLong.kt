package org.oddlama.vane.annotation.config

/**
 * Long integer configuration field with optional bounds.
 *
 * @property def Default value.
 * @property min Minimum accepted value.
 * @property max Maximum accepted value.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigLong(
    val def: Long,
    val min: Long = Long.MIN_VALUE,
    val max: Long = Long.MAX_VALUE,
    val desc: String,
    val metrics: Boolean = true,
)
