package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.core.YamlLoadException

/**
 * Config field handler for double values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigDoubleField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigDouble,
) : ConfigField<Double?>(owner, field, mapName, "double", annotation.desc) {

    /** Returns the default value for this config field. */
    override fun def(): Double = overriddenDef() ?: annotation.def

    /** Returns whether metrics collection is enabled for this field. */
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    /** Generates YAML for this field. */
    override fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?,
    ) {
        appendDescription(builder, indent)
        appendValueRange(builder, indent, annotation.min, annotation.max, Double.NaN, Double.NaN)
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
        if (!yaml.isDouble(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected double")
        validateDoubleRange(yamlPath(), yaml.getDouble(yamlPath()), annotation.min, annotation.max)
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): Double = yaml.getDouble(yamlPath())

    /** Writes the loaded value into the reflected field. */
    override fun load(yaml: YamlConfiguration) {
        try {
            field.setDouble(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
