package org.oddlama.vane.core.lang

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.lang.LangVersion
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator

/**
 * Handles loading and validation for the language file version entry.
 *
 * @param module the owning module.
 * @param owner the target object instance.
 * @param field the reflected target field.
 * @param mapName maps Java field names to YAML paths.
 * @param annotation the source annotation metadata.
 */
class LangVersionField(
    module: Module<*>,
    owner: Any?,
    field: Field,
    mapName: (String?) -> String,
    /** Annotation metadata for this field. */
    var annotation: LangVersion?,
) : LangField<Long?>(module, owner, field, mapName) {

    /** Validates that the YAML version entry exists and is a positive number. */
    @Throws(YamlLoadException::class)
    override fun checkLoadable(yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        checkYamlPath(yaml)
        if (yaml.get(yamlPath()) !is Number)
            throw YamlLoadException.Lang(
                "Invalid type for yaml path '${yamlPath()}', expected long",
                this,
            )
        val value = yaml.getLong(yamlPath())
        if (value < 1)
            throw YamlLoadException.Lang(
                "Entry '${yamlPath()}' has an invalid value: Value must be >= 1",
                this,
            )
    }

    /** Loads the version value into the reflected field. */
    override fun load(namespace: String?, yaml: YamlConfiguration?) {
        if (yaml == null) throw YamlLoadException.Lang("yaml is null", this)
        try {
            field.setLong(owner, yaml.getLong(yamlPath()))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }

    /** Language version entries are not exported as resource pack translations. */
    @Throws(YamlLoadException::class)
    override fun addTranslations(
        pack: ResourcePackGenerator?,
        yaml: YamlConfiguration?,
        langCode: String?,
    ) = Unit
}
