package org.oddlama.vane.annotation.config

import org.bukkit.Material

/**
 * Set of Materials used for configuration purposes.
 *
 * @property def Default material set values.
 * @property desc Human-readable description for docs/config output.
 * @property metrics Whether this field is included in metrics reporting.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ConfigMaterialSet(
    val def: Array<Material>,
    val desc: String,
    val metrics: Boolean = true,
)
