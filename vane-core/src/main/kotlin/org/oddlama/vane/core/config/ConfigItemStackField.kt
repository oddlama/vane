package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.config.ConfigItemStack
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.util.MaterialUtil.materialFrom
import org.oddlama.vane.util.StorageUtil.namespacedKey

/**
 * Config field handler for item stack values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigItemStackField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    var annotation: ConfigItemStack,
) : ConfigField<ItemStack?>(owner, field, mapName, "item stack", annotation.desc) {

    /** Formats an item stack material as a quoted namespaced key. */
    private fun ItemStack.materialKeyString() =
        "\"${escapeYaml(type.key.namespace)}:${escapeYaml(type.key.key)}\""

    /** Appends an item stack definition block. */
    private fun appendItemStackDefinition(
        builder: StringBuilder,
        indent: String?,
        prefix: String?,
        def: ItemStack,
    ) {
        builder.append("$indent$prefix  material: ${def.materialKeyString()}\n")
        if (def.amount != 1) builder.append("$indent$prefix  amount: ${def.amount}\n")
    }

    /** Returns the default value for this config field. */
    override fun def(): ItemStack =
        overriddenDef() ?: ItemStack(annotation.def.type, annotation.def.amount)

    /** Returns whether metrics collection is enabled for this field. */
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    /** Generates YAML for this field. */
    override fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?,
    ) {
        appendDescription(builder, indent)
        builder.append("$indent# Default:\n")
        appendItemStackDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def =
            if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
                loadFromYaml(existingCompatibleConfig)
            else def()
        appendItemStackDefinition(builder, indent, "", def)
    }

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isConfigurationSection(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected group")
        for (varKey in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val varPath = "${yamlPath()}.$varKey"
            when (varKey) {
                "material" -> {
                    if (!yaml.isString(varPath))
                        throw YamlLoadException(
                            "Invalid type for yaml path '$varPath', expected list"
                        )
                    val str = yaml.getString(varPath)!!
                    val (ns, key) = requireNamespacedKeyParts(yamlPath(), str, "material")
                    if (ns.isEmpty() || key.isEmpty())
                        throw YamlLoadException(
                            "Invalid material for yaml path '${yamlPath()}': '$str' is not a valid namespaced key"
                        )
                }

                "amount" -> {
                    if (yaml.get(varPath) !is Number)
                        throw YamlLoadException(
                            "Invalid type for yaml path '${yamlPath()}', expected int"
                        )
                    if (yaml.getInt(varPath) < 0)
                        throw YamlLoadException(
                            "Invalid value for yaml path '${yamlPath()}' Must be >= 0"
                        )
                }
            }
        }
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): ItemStack {
        var materialStr = ""
        var amount = 1
        for (varKey in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val varPath = "${yamlPath()}.$varKey"
            when (varKey) {
                "material" -> {
                    materialStr = yaml.getString(varPath)!!
                    amount = 0
                }

                "amount" -> amount = yaml.getInt(varPath)
            }
        }
        val (ns, key) = requireNamespacedKeyParts(yamlPath(), materialStr, "material")
        return ItemStack(materialFrom(namespacedKey(ns, key))!!, amount)
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
