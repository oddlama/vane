package org.oddlama.vane.annotation.config

/**
 * String configuration field.
 *
 * @property def Default value.
 * @property desc Description used in generated docs/config.
 * @property metrics Whether this field should be recorded in metrics.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigString(val def: String, val desc: String, val metrics: Boolean = false)
