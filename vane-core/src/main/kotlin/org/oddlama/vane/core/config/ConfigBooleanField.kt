package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.core.YamlLoadException

/**
 * Config field handler for boolean values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigBooleanField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    private val annotation: ConfigBoolean,
) : ConfigField<Boolean?>(owner, field, mapName, "boolean", annotation.desc) {

    /** Returns the default value for this config field. */
    override fun def(): Boolean = overriddenDef() ?: annotation.def

    /** Returns whether metrics collection is enabled for this field. */
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    /** Generates YAML for this field. */
    override fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?,
    ) {
        appendDescription(builder, indent)
        appendDefaultValue(builder, indent, def())
        val def =
            if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
                loadFromYaml(existingCompatibleConfig)
            else def()
        appendFieldDefinition(builder, indent, def)
    }

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isBoolean(yamlPath())) {
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected boolean")
        }
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): Boolean {
        return yaml.getBoolean(yamlPath())
    }

    /** Writes the loaded value into the reflected field. */
    override fun load(yaml: YamlConfiguration) {
        try {
            field.setBoolean(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
