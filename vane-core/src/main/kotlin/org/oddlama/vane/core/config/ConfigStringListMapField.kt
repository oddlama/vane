package org.oddlama.vane.core.config

import java.lang.reflect.Field
import java.util.*
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigStringListMap
import org.oddlama.vane.core.YamlLoadException

/**
 * Config field handler for mappings of string keys to string lists.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigStringListMapField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    var annotation: ConfigStringListMap,
) :
    ConfigField<MutableMap<String?, MutableList<String?>?>?>(
        owner,
        field,
        mapName,
        "map of string to string list",
        annotation.desc,
    ) {
    /** Appends the string-list-map definition block to YAML output. */
    private fun appendStringListMapDefinition(
        builder: StringBuilder,
        indent: String?,
        prefix: String?,
        def: MutableMap<String?, MutableList<String?>?>,
    ) {
        def.forEach { (k, list) ->
            builder.append("$indent$prefix  ${escapeYaml(toPascalCase(k))}:\n")
            list!!.forEach { s -> builder.append("$indent$prefix    - ${escapeYaml(s)}\n") }
        }
    }

    /** Returns the default value for this config field. */
    override fun def(): MutableMap<String?, MutableList<String?>?> =
        overriddenDef()
            ?: annotation.def
                .associate { e -> e.key to e.list.toMutableList<String?>() }
                .toMutableMap()

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
        appendStringListMapDefinition(builder, indent, "# ", def())
        builder.append("$indent${basename()}:\n")
        val def =
            if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
                loadFromYaml(existingCompatibleConfig)
            else def()
        appendStringListMapDefinition(builder, indent, "", def)
    }

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isConfigurationSection(yamlPath()))
            throw YamlLoadException("Invalid type for yaml path '${yamlPath()}', expected group")
        for (listKey in yaml.getConfigurationSection(yamlPath())!!.getKeys(false)) {
            val listPath = "${yamlPath()}.$listKey"
            if (!yaml.isList(listPath))
                throw YamlLoadException("Invalid type for yaml path '$listPath', expected list")
            for (obj in yaml.getList(listPath)!!) {
                if (obj !is String)
                    throw YamlLoadException(
                        "Invalid type for yaml path '$listPath', expected string"
                    )
            }
        }
    }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): MutableMap<String?, MutableList<String?>?> =
        yaml.getConfigurationSection(yamlPath())!!.getKeys(false).associateTo(mutableMapOf()) {
            listKey ->
            val listPath = "${yamlPath()}.$listKey"
            normalizeKey(listKey) to yaml.getList(listPath)!!.map { it as String? }.toMutableList()
        }

    /** Writes the loaded value into the reflected field. */
    override fun load(yaml: YamlConfiguration) {
        try {
            field.set(owner, loadFromYaml(yaml))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }

    /** Static key transformation helpers for map serialization. */
    companion object {
        /** Runs of characters that separate words in an internal key. */
        private val KEY_SEPARATORS = Regex("[^A-Za-z0-9]+")

        /** Runs of characters that are folded to a single `_` when normalizing a YAML heading. */
        private val NON_KEY_CHARS = Regex("[^a-z0-9]+")

        /** Converts internal snake/case keys to PascalCase YAML headings. */
        private fun toPascalCase(key: String?): String? {
            if (key.isNullOrEmpty()) return key
            return key.split(KEY_SEPARATORS)
                .filter { it.isNotEmpty() }
                .joinToString("") { it[0].uppercaseChar() + it.substring(1) }
        }

        /** Normalizes YAML headings back to lowercase snake_case keys. */
        private fun normalizeKey(key: String?): String? =
            key?.lowercase(Locale.getDefault())?.replace(NON_KEY_CHARS, "_")
    }
}
