package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigVersion
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module

/**
 * Config field handler for the configuration version entry.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigVersionField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    var annotation: ConfigVersion?,
) :
    ConfigField<Long?>(
        owner,
        field,
        mapName,
        "version id",
        "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated.",
    ) {
    init {
        /** Keeps the version field at the bottom of generated YAML output. */
        this.sortPriority = 100
    }

    /** Version values are generated from module metadata and have no static default. */
    override fun def(): Long? = null

    /** Version reporting is always included in metrics. */
    override fun metrics(): Boolean = true

    /** Generates YAML for the current module config version. */
    override fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?,
    ) {
        appendDescription(builder, indent)
        appendFieldDefinition(builder, indent, (owner as Module<*>).annotation.configVersion)
    }

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (yaml.get(yamlPath()) !is Number) {
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected long")
        }
        val value = yaml.getLong(yamlPath())
        if (value < 1) {
            throw YamlLoadException(
                "Configuration '${yamlPath()}' has an invalid value: Value must be >= 1"
            )
        }
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
