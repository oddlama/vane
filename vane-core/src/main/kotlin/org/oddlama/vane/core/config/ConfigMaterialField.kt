package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigMaterial
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey

/**
 * Config field handler for material values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigMaterialField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    var annotation: ConfigMaterial,
) : ConfigField<Material?>(owner, field, mapName, "material", annotation.desc) {

    /** Returns the default value for this config field. */
    override fun def(): Material = overriddenDef() ?: annotation.def

    /** Returns whether metrics collection is enabled for this field. */
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    /** Formats a material as a quoted namespaced key. */
    private fun Material.keyString() = "\"${escapeYaml(key.namespace)}:${escapeYaml(key.key)}\""

    /** Generates YAML for this field. */
    override fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?,
    ) {
        appendDescription(builder, indent)
        appendDefaultValue(builder, indent, def().keyString())
        val def =
            if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
                loadFromYaml(existingCompatibleConfig)
            else def()
        appendFieldDefinition(builder, indent, def.keyString())
    }

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isString(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected string")
        val str = yaml.getString(yamlPath())
        val (ns, key) = requireNamespacedKeyParts(yamlPath(), str, "material")
        materialFrom(namespacedKey(ns, key))
            ?: throw YamlLoadException(
                "Invalid material entry in list '${yamlPath()}': '$str' does not exist"
            )
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): Material {
        val str = yaml.getString(yamlPath())
        val (ns, key) = requireNamespacedKeyParts(yamlPath(), str, "material")
        return materialFrom(namespacedKey(ns, key))!!
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
