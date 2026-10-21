package org.oddlama.vane.core.lang

import java.lang.reflect.Field
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator

/**
 * Base abstraction for fields annotated as translatable language entries.
 *
 * @param T the runtime value type loaded into the target field.
 * @param module the module that owns this field.
 * @param owner the object instance that contains the reflected field.
 * @param field the reflected field to read/write.
 * @param mapName maps Java field names to YAML paths.
 */
abstract class LangField<T>(
    /** Owning module. */
    private val module: Module<*>,
    /** Reflective owner object containing the language field. */
    protected var owner: Any?,
    /** Reflected language field handle. */
    protected var field: Field,
    mapName: (String?) -> String,
) {
    /** YAML entry name used to load this language value. */
    var name: String
        protected set

    /** Namespace used for translation keys. */
    private val namespace: String

    /** Full translation key used in generated language files. */
    private val key: String

    init {
        if (!field.name.startsWith(PREFIX))
            throw RuntimeException(YamlLoadException.Lang("field must start with $PREFIX", this))
        this.name = mapName(field.name.substring(PREFIX.length))
        this.namespace = module.namespace()
        this.key = "$namespace.${yamlPath()}"
        field.isAccessible = true
    }

    /** Returns the YAML path used to load this field. */
    fun yamlPath(): String = name

    /**
     * Validates that the YAML path exists.
     *
     * @param yaml the configuration to check.
     * @throws YamlLoadException if the path is missing.
     */
    @Throws(YamlLoadException::class)
    protected fun checkYamlPath(yaml: YamlConfiguration) {
        if (!yaml.contains(name, true))
            throw YamlLoadException.Lang("yaml is missing entry with path '$name'", this)
    }

    /** Returns the owning module. */
    fun module(): Module<*> = module

    /** Returns the translation namespace. */
    fun namespace(): String = namespace

    /** Returns the fully qualified translation key. */
    fun key(): String = key

    /**
     * Validates that this field can be loaded from the YAML source.
     *
     * @param yaml the configuration to validate.
     * @throws YamlLoadException if validation fails.
     */
    @Throws(YamlLoadException::class) abstract fun checkLoadable(yaml: YamlConfiguration?)

    /**
     * Loads this field value from YAML into the owner object.
     *
     * @param namespace the active namespace.
     * @param yaml the configuration to read from.
     */
    abstract fun load(namespace: String?, yaml: YamlConfiguration?)

    /**
     * Adds translation values for this field into a resource pack builder.
     *
     * @param pack the target resource pack generator.
     * @param yaml the language configuration source.
     * @param langCode the target language code.
     * @throws YamlLoadException if translation data is invalid.
     */
    @Throws(YamlLoadException::class)
    abstract fun addTranslations(
        pack: ResourcePackGenerator?,
        yaml: YamlConfiguration?,
        langCode: String?,
    )

    /** Returns the current value from the underlying reflected field. */
    @Suppress("UNCHECKED_CAST")
    fun get(): T? =
        try {
            field.get(owner) as T?
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }

    /** Returns a stable debug representation of this reflected language field. */
    override fun toString(): String = "${field.declaringClass.typeName}::${field.name}"

    /** Constants used by language field processing. */
    companion object {
        /** Required field-name prefix for language entries. */
        const val PREFIX: String = "lang"
    }
}
