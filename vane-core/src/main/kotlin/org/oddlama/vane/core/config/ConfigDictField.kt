package org.oddlama.vane.core.config

import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.ConfigDict
import org.oddlama.vane.core.YamlLoadException

/**
 * Config field handler for dictionary-like structured values.
 *
 * @param owner object containing the reflected config field.
 * @param field reflected config field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation source annotation metadata.
 */
class ConfigDictField(
    owner: Any?,
    field: Field,
    mapName: (String?) -> String?,
    /** Annotation metadata for this field. */
    var annotation: ConfigDict,
) : ConfigField<ConfigDictSerializable?>(owner, field, mapName, "dict", annotation.desc) {

    /** Empty default dictionary implementation when no explicit default is provided. */
    private class EmptyDict : ConfigDictSerializable {
        /** Returns an empty dictionary. */
        override fun toDict(): MutableMap<String, Any> = mutableMapOf()

        /** Applies no values. */
        override fun fromDict(dict: MutableMap<String, Any>) = Unit
    }

    /** Returns the default value for this config field. */
    override fun def(): ConfigDictSerializable = overriddenDef() ?: EmptyDict()

    /** Returns whether metrics collection is enabled for this field. */
    override fun metrics(): Boolean = overriddenMetrics() ?: annotation.metrics

    /** Appends a YAML list definition for dictionary values. */
    private fun appendList(
        builder: StringBuilder,
        indent: String?,
        listKey: String?,
        list: MutableList<Any?>,
    ) {
        builder.append("$indent$listKey")
        if (list.isEmpty()) {
            builder.append(": []\n")
        } else {
            builder.append(":\n")
            list.forEach { entry ->
                when (entry) {
                    is String -> builder.append("$indent  - \"${escapeYaml(entry)}\"\n")
                    is Int,
                    is Long,
                    is Float,
                    is Double,
                    is Boolean -> builder.append("$indent  - $entry\n")
                    is MutableMap<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        appendDict(
                            builder,
                            "$indent  ",
                            null,
                            entry as MutableMap<String, Any>,
                            true,
                        )
                    }

                    else ->
                        throw RuntimeException(
                            "Invalid value '$entry' of type ${entry!!.javaClass} in mapping of ConfigDictSerializable"
                        )
                }
            }
        }
    }

    /** Appends a YAML dictionary definition recursively. */
    private fun appendDict(
        builder: StringBuilder,
        indent: String?,
        dictKey: String?,
        dict: MutableMap<String, Any>,
        isListEntry: Boolean,
    ) {
        builder.append(indent)
        if (isListEntry) builder.append("-") else builder.append("$dictKey:")
        if (dict.isEmpty()) {
            builder.append(" {}\n")
        } else {
            builder.append("\n")
            dict.entries
                .sortedBy { it.key }
                .forEach { (k, v) ->
                    when (v) {
                        is String -> builder.append("$indent  $k: \"${escapeYaml(v)}\"\n")
                        is Int,
                        is Long,
                        is Float,
                        is Double,
                        is Boolean -> builder.append("$indent  $k: $v\n")
                        is MutableMap<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            appendDict(builder, "$indent  ", k, v as MutableMap<String, Any>, false)
                        }

                        is MutableList<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            appendList(builder, "$indent  ", k, v as MutableList<Any?>)
                        }

                        else ->
                            throw RuntimeException(
                                "Invalid value '$v' of type ${v.javaClass} in mapping of ConfigDictSerializable"
                            )
                    }
                }
        }
    }

    /** Appends either default or effective dictionary output block. */
    private fun appendDict(
        builder: StringBuilder,
        indent: String?,
        defaultDefinition: Boolean,
        ser: ConfigDictSerializable,
    ) {
        if (defaultDefinition) appendDict(builder, "$indent# ", "Default", ser.toDict(), false)
        else appendDict(builder, indent, basename(), ser.toDict(), false)
    }

    /** Generates YAML for this field. */
    override fun generateYaml(
        builder: StringBuilder,
        indent: String,
        existingCompatibleConfig: YamlConfiguration?,
    ) {
        appendDescription(builder, indent)
        appendDict(builder, indent, true, def())
        val def =
            if (existingCompatibleConfig != null && existingCompatibleConfig.contains(yamlPath()))
                loadFromYaml(existingCompatibleConfig)
            else def()
        appendDict(builder, indent, false, def)
    }

    /** Validates that this field is loadable from YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration) {
        checkYamlPath(yaml)
        if (!yaml.isConfigurationSection(yamlPath()))
            throw YamlLoadException(
                "Invalid type for yaml path '${yamlPath()}', expected configuration section"
            )
    }

    /** Loads a nested list from YAML, recursively decoding dictionary entries. */
    fun loadListFromYaml(rawList: MutableList<*>): MutableList<Any?> =
        rawList.mapTo(mutableListOf()) { e ->
            if (e is ConfigurationSection) loadDictFromYaml(e) else e
        }

    /** Loads a nested dictionary from a YAML section. */
    fun loadDictFromYaml(section: ConfigurationSection): MutableMap<String, Any> =
        section.getKeys(false).associateWithTo(mutableMapOf()) { subkey ->
            when {
                section.isConfigurationSection(subkey) ->
                    loadDictFromYaml(section.getConfigurationSection(subkey)!!)
                section.isList(subkey) -> loadListFromYaml(section.getList(subkey)!!)
                section.isString(subkey) -> section.getString(subkey)!!
                section.isInt(subkey) -> section.getInt(subkey)
                section.isDouble(subkey) -> section.getDouble(subkey)
                section.isBoolean(subkey) -> section.getBoolean(subkey)
                section.isLong(subkey) -> section.getLong(subkey)
                else ->
                    throw IllegalStateException(
                        "Cannot load dict entry '${yamlPath()}.$subkey': unknown type"
                    )
            }
        }

    /** Loads this field value from YAML. */
    fun loadFromYaml(yaml: YamlConfiguration): ConfigDictSerializable {
        try {
            val dict =
                annotation.cls.java.getDeclaredConstructor().newInstance() as ConfigDictSerializable
            dict.fromDict(loadDictFromYaml(yaml.getConfigurationSection(yamlPath())!!))
            return dict
        } catch (e: Exception) {
            when (e) {
                is InstantiationException,
                is IllegalAccessException,
                is IllegalArgumentException,
                is InvocationTargetException,
                is NoSuchMethodException,
                is SecurityException ->
                    throw RuntimeException(
                        "Could not instanciate storage class for ConfigDict: ${annotation.cls}",
                        e,
                    )

                else -> throw e
            }
        }
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
