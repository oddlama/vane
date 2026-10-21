package org.oddlama.vane.core.config

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigDoubleList
import org.oddlama.vane.core.YamlLoadException

/**
 * Config field handler for double list values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigDoubleListField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    var annotation: ConfigDoubleList,
) : ConfigField<MutableList<Double?>?>(owner, field, mapName, "double list", annotation.desc) {

    /** Appends a double list definition block. */
    private fun appendDoubleListDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        def: MutableList<Double?>,
    ) {
        appendListDefinition<Double?>(builder, indent, prefix, def) { b, d -> b!!.append(d!!) }
    }

    /** Returns the default value for this config field. */
    override fun def(): MutableList<Double?> =
        overriddenDef() ?: annotation.def.mapTo(mutableListOf()) { it }

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
        builder.append("$indent# Default:\n")
        appendDoubleListDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def =
            if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
                loadFromYaml(existingCompatibleConfig)
            else def()
        appendDoubleListDefinition(builder, indent, "", def)
    }

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isList(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected list")
        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is Number)
                throw YamlLoadException(
                    "Invalid type for yaml path '${yamlPath()}', expected double"
                )
            validateDoubleRange(yamlPath(), obj.toDouble(), annotation.min, annotation.max)
        }
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): MutableList<Double?> =
        yaml.getList(yamlPath())!!.map { (it as Number).toDouble() as Double? }.toMutableList()

    /** Writes the loaded value into the reflected field. */
    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
