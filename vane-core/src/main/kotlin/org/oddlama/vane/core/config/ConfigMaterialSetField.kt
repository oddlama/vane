package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bstats.bukkit.Metrics
import org.bstats.charts.AdvancedPie
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigMaterialSet
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey

/**
 * Config field handler for material set values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigMaterialSetField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    var annotation: ConfigMaterialSet,
) : ConfigField<MutableSet<Material>?>(owner, field, mapName, "set of materials", annotation.desc) {

    /** Formats a material as a quoted namespaced key. */
    private fun Material.keyString() = "\"${escapeYaml(key.namespace)}:${escapeYaml(key.key)}\""

    /** Appends a material set definition block. */
    private fun appendMaterialSetDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        def: MutableSet<Material>,
    ) {
        appendListDefinition(builder, indent, prefix, def) { b, m -> b!!.append(m!!.keyString()) }
    }

    /** Returns the default value for this config field. */
    override fun def(): MutableSet<Material> = overriddenDef() ?: annotation.def.toMutableSet()

    /** Returns whether metrics collection is enabled for this field. */
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    /** Registers an advanced pie metric containing selected material keys. */
    override fun registerMetrics(metrics: Metrics?) {
        if (metrics == null || !this.metrics()) return
        metrics.addCustomChart(
            AdvancedPie(yamlPath()) {
                get()!!.associate { it.key.toString() to 1 }
            }
        )
    }

    /** Generates YAML for this field. */
    override fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?,
    ) {
        appendDescription(builder, indent)
        builder.append("$indent# Default:\n")
        appendMaterialSetDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def: MutableSet<Material> =
            if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
                loadFromYaml(existingCompatibleConfig)
            else def()
        appendMaterialSetDefinition(builder, indent, "", def)
    }

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isList(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected list")
        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is String)
                throw YamlLoadException(
                    "Invalid type for yaml path '${yamlPath()}', expected string"
                )
            val (ns, key) = requireNamespacedKeyParts(yamlPath(), obj, "material")
            materialFrom(namespacedKey(ns, key))
                ?: throw YamlLoadException(
                    "Invalid material entry in list '${yamlPath()}': '$obj' does not exist"
                )
        }
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): MutableSet<Material> =
        yaml.getList(yamlPath())!!.mapTo(mutableSetOf()) { obj ->
            val (ns, key) = requireNamespacedKeyParts(yamlPath(), obj as String, "material")
            materialFrom(namespacedKey(ns, key))!!
        }

    /** Writes the loaded value into the reflected field. */
    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
