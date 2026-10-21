package org.oddlama.vane.annotation.config

/**
 * List of integers configuration field with optional bounds.
 *
 * @property def Default list values.
 * @property min Minimum accepted element value.
 * @property max Maximum accepted element value.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigIntList(
    val def: IntArray,
    val min: Int = Int.MIN_VALUE,
    val max: Int = Int.MAX_VALUE,
    val desc: String,
    val metrics: Boolean = true,
)
