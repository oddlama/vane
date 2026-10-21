package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigIntList
import org.oddlama.vane.core.YamlLoadException

/**
 * Config field handler for integer list values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigIntListField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    var annotation: ConfigIntList,
) : ConfigField<MutableList<Int?>?>(owner, field, mapName, "int list", annotation.desc) {

    /** Appends an integer list definition block. */
    private fun appendIntListDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        def: MutableList<Int?>,
    ) {
        appendListDefinition<Int?>(builder, indent, prefix, def) { b, i -> b!!.append(i!!) }
    }

    /** Returns the default value for this config field. */
    override fun def(): MutableList<Int?> =
        overriddenDef() ?: annotation.def.map { it as Int? }.toMutableList()

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
            Int.MIN_VALUE,
            Int.MAX_VALUE,
        )
        builder.append("$indent# Default:\n")
        appendIntListDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def =
            if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
                loadFromYaml(existingCompatibleConfig)
            else def()
        appendIntListDefinition(builder, indent, "", def)
    }

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isList(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected list")
        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is Number)
                throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected int")
            validateIntRange(yamlPath(), obj.toInt(), annotation.min, annotation.max)
        }
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): MutableList<Int?> =
        yaml.getList(yamlPath())!!.map { (it as Number).toInt() as Int? }.toMutableList()

    /** Writes the loaded value into the reflected field. */
    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
