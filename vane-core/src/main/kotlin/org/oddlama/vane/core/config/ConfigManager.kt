package org.oddlama.vane.core.config

import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.logging.Level
import kotlin.math.max
import org.bstats.bukkit.Metrics
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.*
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.util.LegacyKeys
import org.oddlama.vane.util.ReflectionUtil
import org.oddlama.vane.util.TextUtil

/**
 * Discovers, validates, loads, and regenerates module configuration fields.
 *
 * @param module the module whose config is managed.
 */
class ConfigManager(var module: Module<*>) {
    /**
     * Compiled config field handlers.
     */
    private val configFields: MutableList<ConfigField<*>> = mutableListOf()

    /**
     * Optional section descriptions keyed by YAML path.
     */
    private val sectionDescriptions: MutableMap<String?, String?> = mutableMapOf()

    /**
     * The compiled `@ConfigVersion` field, if present.
     */
    var fieldVersion: ConfigVersionField? = null

    init {
        compile(module) { it }
    }

    /**
     * Returns the expected config version from module metadata.
     */
    fun expectedVersion(): Long = module.annotation.configVersion

    /**
     * Returns whether a reflected field has a config annotation.
     */
    private fun hasConfigAnnotation(field: Field): Boolean =
        field.annotations.any { it.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.config.Config") }

    /**
     * Verifies config fields use the required `config` prefix.
     */
    private fun assertFieldPrefix(field: Field) {
        if (!field.name.startsWith("config")) {
            throw RuntimeException("Configuration fields must be prefixed config. This is a bug.")
        }
    }

    /**
     * Compiles a reflected config field into a concrete [ConfigField] implementation.
     */
    private fun compileField(owner: Any?, field: Field, mapName: (String?) -> String?): ConfigField<*> {
        assertFieldPrefix(field)

        val annotation = field.annotations
            .filter { it.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.config.Config") }
            .also { require(it.size <= 1) { "Configuration fields must have exactly one @Config annotation." } }
            .firstOrNull() ?: error("No @Config annotation found on field ${field.name}")

        return when (val atype = annotation.annotationClass.java) {
            ConfigBoolean::class.java -> ConfigBooleanField(owner, field, mapName, annotation as ConfigBoolean)
            ConfigDict::class.java -> ConfigDictField(owner, field, mapName, annotation as ConfigDict)
            ConfigDouble::class.java -> ConfigDoubleField(owner, field, mapName, annotation as ConfigDouble)
            ConfigDoubleList::class.java -> ConfigDoubleListField(owner, field, mapName, annotation as ConfigDoubleList)
            ConfigExtendedMaterial::class.java -> ConfigExtendedMaterialField(
                owner,
                field,
                mapName,
                annotation as ConfigExtendedMaterial
            )

            ConfigInt::class.java -> ConfigIntField(owner, field, mapName, annotation as ConfigInt)
            ConfigIntList::class.java -> ConfigIntListField(owner, field, mapName, annotation as ConfigIntList)
            ConfigItemStack::class.java -> ConfigItemStackField(owner, field, mapName, annotation as ConfigItemStack)
            ConfigLong::class.java -> ConfigLongField(owner, field, mapName, annotation as ConfigLong)
            ConfigMaterial::class.java -> ConfigMaterialField(owner, field, mapName, annotation as ConfigMaterial)
            ConfigMaterialMapMapMap::class.java -> ConfigMaterialMapMapMapField(
                owner,
                field,
                mapName,
                annotation as ConfigMaterialMapMapMap
            )

            ConfigMaterialSet::class.java -> ConfigMaterialSetField(
                owner,
                field,
                mapName,
                annotation as ConfigMaterialSet
            )

            ConfigString::class.java -> ConfigStringField(owner, field, mapName, annotation as ConfigString)
            ConfigStringList::class.java -> ConfigStringListField(owner, field, mapName, annotation as ConfigStringList)
            ConfigStringListMap::class.java -> ConfigStringListMapField(
                owner,
                field,
                mapName,
                annotation as ConfigStringListMap
            )

            ConfigVersion::class.java -> {
                require(owner === module) { "@ConfigVersion can only be used inside the main module. This is a bug." }
                require(fieldVersion == null) { "There must be exactly one @ConfigVersion field! (found multiple). This is a bug." }
                ConfigVersionField(owner, field, mapName, annotation as ConfigVersion).also { fieldVersion = it }
            }

            else -> throw RuntimeException("Missing ConfigField handler for @${atype.name}. This is a bug.")
        }
    }

    /**
     * Verifies a config version and logs actionable upgrade/downgrade guidance.
     */
    private fun verifyVersion(file: File, version: Long): Boolean {
        if (version == expectedVersion()) return true

        module.log.severe("${file.name}: expected version ${expectedVersion()}, but got $version")
        when {
            version == 0L -> {
                module.log.severe("Something went wrong while generating or loading the configuration.")
                module.log.severe("If you are sure your configuration is correct and this isn't a file")
                module.log.severe("system permission issue, please report this to https://github.com/oddlama/vane/issues")
            }

            version < expectedVersion() -> {
                module.log.severe("This config is for an older version of ${module.name}.")
                module.log.severe("Please update your configuration. A new default configuration")
                module.log.severe("has been generated as 'config.yml.new'. Alternatively you can")
                module.log.severe("delete your configuration to have a new one generated next time.")
                generateFile(File(module.dataFolder, "config.yml.new"), null)
            }

            else -> {
                module.log.severe("This config is for a future version of ${module.name}.")
                module.log.severe("Please use the correct file for this version, or delete it and")
                module.log.severe("it will be regenerated next time the server is started.")
            }
        }
        return false
    }

    /**
     * Registers a YAML section description.
     */
    fun addSectionDescription(yamlPath: String?, description: String?) {
        sectionDescriptions[yamlPath] = description
    }

    /**
     * Discovers and compiles config fields on an owner object.
     *
     * @param owner object containing config annotations.
     * @param mapName maps Java field names to YAML paths.
     */
    fun compile(owner: Any, mapName: (String?) -> String?) {
        configFields.addAll(
            ReflectionUtil.allFields(owner.javaClass)
                .filter { hasConfigAnnotation(it) }
                .map { compileField(owner, it, mapName) }
        )
        configFields.sort()

        if (owner === module && fieldVersion == null) {
            throw RuntimeException("There must be exactly one @ConfigVersion field! (found none). This is a bug.")
        }
    }

    /**
     * Returns indentation spaces for a YAML depth level.
     */
    private fun indentStr(level: Int): String = "  ".repeat(level)

    /**
     * Generates YAML content for all compiled config fields.
     *
     * @param builder output builder.
     * @param existingCompatibleConfig existing config values to retain where possible.
     */
    fun generateYaml(builder: StringBuilder, existingCompatibleConfig: YamlConfiguration?) {
        builder.append("# vim: set tabstop=2 softtabstop=0 expandtab shiftwidth=2:\n")
        builder.append("# This config file will automatically be updated, as long\n")
        builder.append("# as there are no incompatible changes between versions.\n")
        builder.append("# This means that additional comments will not be preserved!\n")

        var lastField: ConfigField<*>? = fieldVersion
        var indent = ""

        for (f in configFields) {
            builder.append("\n")

            if (!ConfigField.sameGroup(lastField, f)) {
                val newIndentLevel = f.groupCount()
                val commonIndentLevel = ConfigField.commonGroupCount(lastField, f)

                var sectionPath: String? = ""
                for (i in 0 until commonIndentLevel) {
                    sectionPath = Context.appendYamlPath(sectionPath!!, f.components()[i], ".")
                }

                for (i in commonIndentLevel until newIndentLevel) {
                    indent = indentStr(i)
                    sectionPath = Context.appendYamlPath(sectionPath!!, f.components()[i], ".")

                    sectionDescriptions[sectionPath]?.let { sectionDesc ->
                        val wrapped = TextUtil.wordWrap(sectionDesc, max(60, 80 - indent.length), "\n$indent# ")
                        builder.append("$indent# $wrapped\n")
                    }

                    builder.append("$indent${f.components()[i]}:\n")
                }
                indent = indentStr(newIndentLevel)
            }

            f.generateYaml(builder, indent, existingCompatibleConfig)
            lastField = f
        }
    }

    /**
     * Returns the module's canonical `config.yml` path.
     */
    fun standardFile(): File = File(module.dataFolder, "config.yml")

    /**
     * Generates a config file atomically.
     *
     * @param file destination file.
     * @param existingCompatibleConfig existing config values to retain where possible.
     * @return `true` if file generation succeeded.
     */
    fun generateFile(file: File, existingCompatibleConfig: YamlConfiguration?): Boolean {
        val content = buildString { generateYaml(this, existingCompatibleConfig) }
        val tmpFile = File("${file.absolutePath}.tmp")
        try {
            Files.writeString(tmpFile.toPath(), content)
        } catch (e: IOException) {
            module.log.log(Level.SEVERE, "error while writing config file '$file'", e)
            return false
        }
        try {
            Files.move(
                tmpFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: IOException) {
            module.log.log(
                Level.SEVERE,
                "error while atomically replacing '$file' with temporary file (very recent changes might be lost)!",
                e
            )
            return false
        }
        return true
    }

    /**
     * Converts a config written before v1.22.0 (snake_case keys such as `resource_pack.force`)
     * to the current paths (`ResourcePack.Force`), after backing up [file]. The caller then
     * regenerates the file from the result.
     *
     * @return the converted config, or `null` if the backup could not be written.
     */
    private fun migrateLegacyKeys(file: File, legacy: YamlConfiguration): YamlConfiguration? {
        try {
            val backup = LegacyKeys.backup(file)
            module.log.info("Migrating legacy keys in '${file.name}' (backup: '${backup.name}')")
        } catch (e: IOException) {
            module.log.log(Level.SEVERE, "Not migrating '${file.name}': could not back it up", e)
            return null
        }

        return LegacyKeys.migrateYaml(legacy, configFields.map { it.yamlPath() })
    }

    /**
     * Reloads and normalizes configuration from disk.
     *
     * @param file source config file.
     * @return `true` when reload and field population succeed.
     */
    fun reload(file: File): Boolean {
        var yaml = YamlConfiguration.loadConfiguration(file)
        if (!yaml.contains("Version") && yaml.contains("version")) {
            yaml = migrateLegacyKeys(file, yaml) ?: return false
        }

        val version = yaml.getLong("Version", -1)
        if (!verifyVersion(file, version)) return false

        val tmpFile = File(module.dataFolder, "config.yml.tmp")
        try {
            // Regenerating loads every field, so invalid entries are reported here, before the
            // file on disk is replaced.
            if (!generateFile(tmpFile, yaml)) return false
        } catch (e: YamlLoadException) {
            module.log.log(Level.SEVERE, "error while loading '${file.name}'", e)
            return false
        }

        try {
            Files.move(
                tmpFile.toPath(),
                standardFile().toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: IOException) {
            module.log.log(
                Level.SEVERE,
                "error while atomically replacing '${standardFile()}' with updated version. " +
                        "Please manually resolve the conflict (new file is named '$tmpFile')", e
            )
            return false
        }

        yaml = YamlConfiguration.loadConfiguration(file)

        try {
            configFields.forEach { it.checkLoadable(yaml) }
            configFields.forEach { it.load(yaml) }
        } catch (e: YamlLoadException) {
            module.log.log(Level.SEVERE, "error while loading '${file.name}'", e)
            return false
        }

        return true
    }

    /**
     * Registers metrics charts for opted-in config fields.
     */
    fun registerMetrics(metrics: Metrics?) {
        configFields.forEach { it.registerMetrics(metrics) }
    }
}