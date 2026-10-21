package org.oddlama.vane.core.lang

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.lang.LangMessageArray
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator

/**
 * Handles loading and exporting a [TranslatedMessageArray].
 *
 * @param module the owning module.
 * @param owner the target object instance.
 * @param field the reflected target field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation the source annotation metadata.
 */
class LangMessageArrayField(
    module: Module<*>,
    owner: Any?,
    field: Field,
    mapName: (String?) -> String,
    /** Annotation metadata for this field. */
    var annotation: LangMessageArray?,
) : LangField<TranslatedMessageArray?>(module, owner, field, mapName) {

    /** Validates this message array entry in YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        checkYamlPath(yaml)
        if (!yaml.isList(yamlPath()))
            throw YamlLoadException.Lang(
                "Invalid type for yaml path '${yamlPath()}', expected list",
                this,
            )
        for (obj in yaml.getList(yamlPath())!!) {
            if (obj !is String)
                throw YamlLoadException.Lang(
                    "Invalid type for yaml path '${yamlPath()}', expected string",
                    this,
                )
        }
    }

    /** Reads the message list from YAML. */
    private fun fromYaml(yaml: YamlConfiguration): MutableList<String> =
        yaml.getList(yamlPath())!!.map { it as String }.toMutableList()

    /** Loads the translated message array into the reflected field. */
    override fun load(namespace: String?, yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        try {
            field.set(owner, TranslatedMessageArray(module(), key(), fromYaml(yaml)))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }

    /** Exports this message array to the generated resource pack translations. */
    @Throws(YamlLoadException::class)
    override fun addTranslations(
        pack: ResourcePackGenerator?,
        yaml: YamlConfiguration?,
        langCode: String?,
    ) {
        if (pack == null || yaml == null || langCode == null) return
        checkLoadable(yaml)
        val list = fromYaml(yaml)
        val loadedSize = get()!!.size()
        if (list.size != loadedSize) {
            throw YamlLoadException.Lang(
                "All translation lists for message arrays must have the exact same size. " +
                    "The loaded language file has $loadedSize entries, while the currently processed file has ${list.size}",
                this,
            )
        }
        list.forEachIndexed { i, entry ->
            pack.translations(namespace(), langCode).put("${key()}.$i", entry)
        }
    }
}
