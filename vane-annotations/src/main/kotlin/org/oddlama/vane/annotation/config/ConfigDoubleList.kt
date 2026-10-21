package org.oddlama.vane.annotation.config

/**
 * List of double values configuration field with optional bounds.
 *
 * @property def Default list values.
 * @property min Minimum accepted element value, or `NaN` for unbounded.
 * @property max Maximum accepted element value, or `NaN` for unbounded.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigDoubleList(
    val def: DoubleArray,
    val min: Double = Double.NaN,
    val max: Double = Double.NaN,
    val desc: String,
    val metrics: Boolean = true,
)
