package org.oddlama.vane.core.lang

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator

/**
 * Handles loading and exporting a single [TranslatedMessage].
 *
 * @param module the owning module.
 * @param owner the target object instance.
 * @param field the reflected target field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation the source annotation metadata.
 */
class LangMessageField(
    module: Module<*>,
    owner: Any?,
    field: Field,
    mapName: (String?) -> String,
    /** Annotation metadata for this field. */
    var annotation: LangMessage?,
) : LangField<TranslatedMessage?>(module, owner, field, mapName) {

    /** Validates this message entry in YAML. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        checkYamlPath(yaml)

        if (!yaml.isString(yamlPath())) {
            throw YamlLoadException.Lang(
                "Invalid type for yaml path '${yamlPath()}', expected string",
                this,
            )
        }
    }

    /** Reads the message string from YAML. */
    private fun fromYaml(yaml: YamlConfiguration): String = yaml.getString(yamlPath()) ?: ""

    /** Loads the translated message into the reflected field. */
    override fun load(namespace: String?, yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        try {
            field.set(owner, TranslatedMessage(module(), key(), fromYaml(yaml)))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }

    /** Exports this message to the generated resource pack translations. */
    @Throws(YamlLoadException::class)
    override fun addTranslations(
        pack: ResourcePackGenerator?,
        yaml: YamlConfiguration?,
        langCode: String?,
    ) {
        if (pack == null || yaml == null || langCode == null) return
        checkLoadable(yaml)
        pack.translations(namespace(), langCode).put(key(), fromYaml(yaml))
    }
}
