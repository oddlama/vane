package org.oddlama.vane.annotation.config

/**
 * Extended material configuration using a string identifier for custom materials.
 *
 * @property def Default material identifier.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigExtendedMaterial(
    val def: String,
    val desc: String,
    val metrics: Boolean = true,
)
