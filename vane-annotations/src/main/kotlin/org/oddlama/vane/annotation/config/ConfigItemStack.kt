package org.oddlama.vane.annotation.config

/**
 * ItemStack configuration field with a default defined by [ConfigItemStackDef].
 *
 * @property def Default item stack specification.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigItemStack(
    val def: ConfigItemStackDef,
    val desc: String,
    val metrics: Boolean = true,
)
