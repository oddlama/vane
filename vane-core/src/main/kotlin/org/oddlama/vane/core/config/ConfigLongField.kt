package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.core.YamlLoadException

/**
 * Config field handler for long values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigLongField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    var annotation: ConfigLong,
) : ConfigField<Long?>(owner, field, mapName, "long", annotation.desc) {

    /** Returns the default value for this config field. */
    override fun def(): Long = overriddenDef() ?: annotation.def

    /** Returns whether metrics collection is enabled for this field. */
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    /** Generates YAML for this field. */
    override fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?,
    ) {
        appendDescription(builder, indent)
        appendValueRange(
            builder,
            indent,
            annotation.min,
            annotation.max,
            Long.MIN_VALUE,
            Long.MAX_VALUE,
        )
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
        if (yaml.get(yamlPath()) !is Number)
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected long")
        val value = yaml.getLong(yamlPath())
        if (annotation.min != Long.MIN_VALUE && value < annotation.min)
            throw YamlLoadException(
                "Configuration '${yamlPath()}' has an invalid value: Value must be >= ${annotation.min}"
            )
        if (annotation.max != Long.MAX_VALUE && value > annotation.max)
            throw YamlLoadException(
                "Configuration '${yamlPath()}' has an invalid value: Value must be <= ${annotation.max}"
            )
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): Long = yaml.getLong(yamlPath())

    /** Writes the loaded value into the reflected field. */
    override fun load(yaml: YamlConfiguration) {
        try {
            field.setLong(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
