package org.oddlama.vane.core.lang

import java.io.File
import java.lang.reflect.Field
import java.util.logging.Level
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.lang.LangMessageArray
import org.oddlama.vane.annotation.lang.LangVersion
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import org.oddlama.vane.util.ReflectionUtil

/**
 * Discovers, validates, and loads language fields for a module.
 *
 * @param module the module whose language entries are managed.
 */
class LangManager(var module: Module<*>) {
    /** All compiled language field handlers. */
    private val langFields: MutableList<LangField<*>> = mutableListOf()

    /** The compiled `@LangVersion` field, if discovered. */
    var fieldVersion: LangVersionField? = null

    init {
        compile(module) { it ?: "" }
    }

    /** Returns the expected language file version from module metadata. */
    fun expectedVersion(): Long = module.annotation.langVersion

    /** Returns whether a field has any supported language annotation. */
    private fun hasLangAnnotation(field: Field): Boolean =
        field.annotations.any {
            it.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.lang.Lang")
        }

    /** Ensures language fields follow the required `lang` prefix convention. */
    private fun assertFieldPrefix(field: Field) {
        if (!field.name.startsWith("lang"))
            throw RuntimeException("Language fields must be prefixed lang. This is a bug.")
    }

    /** Compiles a reflected language field into a concrete [LangField] handler. */
    private fun compileField(
        owner: Any?,
        field: Field,
        mapName: (String?) -> String,
    ): LangField<*> {
        assertFieldPrefix(field)

        val annotation =
            field.annotations
                .filter {
                    it.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.lang.Lang")
                }
                .also {
                    require(it.size <= 1) {
                        "Language fields must have exactly one @Lang annotation."
                    }
                }
                .firstOrNull() ?: error("No @Lang annotation found on field ${field.name}")

        return when (val atype = annotation.annotationClass.java) {
            LangMessage::class.java ->
                LangMessageField(module, owner, field, mapName, annotation as LangMessage)
            LangMessageArray::class.java ->
                LangMessageArrayField(
                    module,
                    owner,
                    field,
                    mapName,
                    annotation as LangMessageArray,
                )

            LangVersion::class.java -> {
                require(owner === module) {
                    "@LangVersion can only be used inside the main module. This is a bug."
                }
                require(fieldVersion == null) {
                    "There must be exactly one @LangVersion field! (found multiple). This is a bug."
                }
                LangVersionField(module, owner, field, mapName, annotation as LangVersion).also {
                    fieldVersion = it
                }
            }

            else ->
                throw RuntimeException(
                    "Missing LangField handler for @${atype.name}. This is a bug."
                )
        }
    }

    /** Verifies a language file version against the expected module version. */
    private fun verifyVersion(file: File, version: Long): Boolean {
        if (version == expectedVersion()) return true

        module.log.severe("${file.name}: expected version ${expectedVersion()}, but got $version")
        when {
            version == 0L -> {
                module.log.severe(
                    "Something went wrong while generating or loading the configuration."
                )
                module.log.severe(
                    "If you are sure your configuration is correct and this isn't a file"
                )
                module.log.severe(
                    "system permission issue, please report this to https://github.com/oddlama/vane/issues"
                )
            }

            version < expectedVersion() -> {
                module.log.severe("This language file is for an older version of ${module.name}.")
                module.log.severe(
                    "Please update your file or use an officially supported language file."
                )
            }

            else -> {
                module.log.severe("This language file is for a future version of ${module.name}.")
                module.log.severe(
                    "Please use the correct file for this version, or use an officially"
                )
                module.log.severe("supported language file.")
            }
        }
        return false
    }

    /**
     * Discovers and compiles language fields from an owner instance.
     *
     * @param owner the object containing language fields.
     * @param mapName maps Java field names to YAML paths.
     */
    fun compile(owner: Any, mapName: (String?) -> String) {
        langFields.addAll(
            ReflectionUtil.allFields(owner.javaClass)
                .filter { hasLangAnnotation(it) }
                .map { compileField(owner, it, mapName) }
        )
        if (owner === module && fieldVersion == null)
            throw RuntimeException(
                "There must be exactly one @LangVersion field! (found none). This is a bug."
            )
    }

    /** Returns a compiled language field by mapped name. */
    fun getField(name: String?): LangField<*> =
        langFields.firstOrNull { it.name == name }
            ?: throw RuntimeException("Missing lang field lang$name")

    /**
     * Reloads language values from a language file.
     *
     * @param file the language YAML file.
     * @return `true` on success.
     */
    fun reload(file: File): Boolean {
        val yaml = YamlConfiguration.loadConfiguration(file)
        val version = yaml.getLong("Version", -1)
        if (!verifyVersion(file, version)) return false

        try {
            langFields.forEach { it.checkLoadable(yaml) }
            langFields.forEach { it.load(module.namespace(), yaml) }
        } catch (e: YamlLoadException) {
            module.log.log(Level.SEVERE, "error while loading '${file.absolutePath}'", e)
            return false
        }
        return true
    }

    /**
     * Adds translatable entries from a language file to a resource pack generator.
     *
     * @param pack the target resource pack generator.
     * @param yaml the loaded language configuration.
     * @param langFile the source language file.
     */
    fun generateResourcePack(
        pack: ResourcePackGenerator?,
        yaml: YamlConfiguration,
        langFile: File,
    ) {
        val langCode =
            yaml.getString("ResourcePackLangCode")
                ?: throw RuntimeException("Missing yaml key: ResourcePackLangCode")

        val errors = mutableListOf<YamlLoadException.Lang>()
        for (f in langFields) {
            try {
                f.addTranslations(pack, yaml, langCode)
            } catch (e: YamlLoadException.Lang) {
                errors.add(e)
            } catch (e: YamlLoadException) {
                module.log.log(Level.SEVERE, "Unexpected YAMLLoadException: ", e)
            }
        }
        if (errors.isNotEmpty()) {
            val erroredLangNodes = errors.joinToString("\n\t\t") { it.message }
            module.log.log(
                Level.SEVERE,
                "The following errors were identified while adding translations from \n\t${langFile.absolutePath} \n\t\t$erroredLangNodes",
            )
        }
    }
}
