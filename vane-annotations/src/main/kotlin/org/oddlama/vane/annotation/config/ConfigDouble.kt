package org.oddlama.vane.annotation.config

/**
 * Double configuration field with optional bounds.
 *
 * @property def Default value.
 * @property min Minimum accepted value, or `NaN` for unbounded.
 * @property max Maximum accepted value, or `NaN` for unbounded.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigDouble(
    val def: Double,
    val min: Double = Double.NaN,
    val max: Double = Double.NaN,
    val desc: String,
    val metrics: Boolean = true,
)
