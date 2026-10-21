package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigStringList
import org.oddlama.vane.core.YamlLoadException

/**
 * Config field handler for string list values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigStringListField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    var annotation: ConfigStringList,
) : ConfigField<MutableList<String?>?>(owner, field, mapName, "list of strings", annotation.desc) {

    /** Appends a quoted string list definition block. */
    private fun appendStringListDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        def: MutableList<String?>,
    ) {
        appendListDefinition<String?>(builder, indent, prefix, def) { b, s ->
            b!!.append("\"${escapeYaml(s)}\"")
        }
    }

    /** Returns the default value for this config field. */
    override fun def(): MutableList<String?> =
        overriddenDef() ?: annotation.def.map { it as String? }.toMutableList()

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
        appendStringListDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def =
            if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
                loadFromYaml(existingCompatibleConfig)
            else def()
        appendStringListDefinition(builder, indent, "", def)
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
        }
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): MutableList<String?> =
        yaml.getList(yamlPath())!!.map { it as String? }.toMutableList()

    /** Writes the loaded value into the reflected field. */
    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
