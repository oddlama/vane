package org.oddlama.vane.core.persistent

import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.function.Consumer
import java.util.logging.Level
import org.json.JSONException
import org.json.JSONObject
import org.oddlama.vane.annotation.persistent.Persistent
import java.util.logging.Logger
import org.oddlama.vane.core.persistent.PersistentStorageManager.Migration
import org.oddlama.vane.util.LegacyKeys
import org.oddlama.vane.util.ReflectionUtil

/**
 * Filesystem persistence independent of the Bukkit plugin lifecycle, so failure paths can be tested
 * against real files. The manager supplies the current module's version, path and logger.
 */
internal class PersistentStorage(
    private val currentVersion: () -> Long,
    private val versionPath: () -> String?,
    private val logger: () -> Logger,
    private val replaceFile: (Path, Path) -> Unit = { source, target ->
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    },
) {
    private val log: Logger get() = logger()

    /** Compiled persistent fields discovered through reflection. */
    private val persistentFields: MutableList<PersistentField> = mutableListOf()

    /** Registered storage migrations. */
    private val migrations: MutableList<Migration> = mutableListOf()

    /** Whether persistent data has been loaded successfully. */
    var isLoaded: Boolean = false

    /** Returns whether a field has a persistent annotation. */
    private fun hasPersistentAnnotation(field: Field): Boolean =
        field.annotations.any {
            it.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.persistent.Persistent")
        }

    /** Verifies that persistent fields use the required `storage` prefix. */
    private fun assertFieldPrefix(field: Field) {
        if (!field.name.startsWith("storage"))
            throw RuntimeException("Configuration fields must be prefixed storage. This is a bug.")
    }

    /** Compiles a reflected field into a [PersistentField] handler. */
    private fun compileField(owner: Any?, field: Field, mapName: (String?) -> String?): PersistentField {
        assertFieldPrefix(field)

        val annotation = field.annotations
            .filter { it.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.persistent.Persistent") }
            .also { require(it.size <= 1) { "Persistent fields must have exactly one @Persistent annotation." } }
            .firstOrNull() ?: error("No @Persistent annotation found on field ${field.name}")

        return when (val atype = annotation.annotationClass.java) {
            Persistent::class.java -> PersistentField(owner, field, mapName)
            else -> throw RuntimeException("Missing PersistentField handler for @${atype.name}. This is a bug.")
        }
    }

    /** Discovers and compiles persistent fields on the given owner object. */
    fun compile(owner: Any, mapName: (String?) -> String?) {
        persistentFields.addAll(
            ReflectionUtil.allFields(owner.javaClass)
                .filter { hasPersistentAnnotation(it) }
                .map { compileField(owner, it, mapName) }
        )
    }

    /** Registers a migration that upgrades storage to a target version. */
    fun addMigrationTo(to: Long, name: String?, migrator: Consumer<JSONObject?>) {
        migrations.add(Migration(to, name, migrator))
    }

    /** Loads persistent data from disk and applies pending migrations. */
    fun load(file: File): Boolean {
        val wasLoaded = isLoaded
        // A failed reload must also revoke permission to overwrite the file.
        isLoaded = false
        if (!file.exists() && wasLoaded) {
            log.severe("Cannot reload persistent storage from nonexistent file '${file.name}'")
            return false
        }

        val json: JSONObject = if (file.exists()) {
            try {
                JSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8))
            } catch (e: IOException) {
                log.log(Level.SEVERE, "error while loading persistent data from '${file.name}'", e)
                return false
            } catch (e: JSONException) {
                log.log(Level.SEVERE, "persistent data in '${file.name}' is not valid JSON", e)
                return false
            }
        } else {
            JSONObject()
        }

        val versionPath = requireNotNull(versionPath())
        val neededVersion = currentVersion()
        // Inspect every spelling before backups, alias migration, migrations or field assignment.
        val version = try {
            checkedVersion(json, versionPath, neededVersion)
        } catch (e: IOException) {
            log.log(Level.SEVERE, "Refusing persistent storage '${file.name}': ${e.message}", e)
            return false
        }
        if (!json.has(versionPath) && !migrateLegacyKeys(file, json, versionPath)) return false
        try {
            if (version != neededVersion && migrations.isNotEmpty()) {
                log.info("Persistent storage is out of date.")
                log.info("§dMigrating storage from version §b$version → $neededVersion§d:")

                // A migration to version N has already run for storage at version N.
                migrations
                    .filter { it.to > version && it.to <= neededVersion }
                    .sortedBy { it.to }
                    .forEach { m ->
                        log.info("  → §b${m.to}§r : Applying migration '§a${m.name}§r'")
                        m.migrator.accept(json)
                    }
            }

            json.put(versionPath, neededVersion.toString())

            persistentFields.forEach { f ->
                if (version == 0L && !json.has(f.path())) return@forEach
                f.load(json)
            }
        } catch (e: Exception) {
            // Includes invalid enum values/type mismatches and failed migrations, not just I/O.
            log.log(Level.SEVERE, "error while loading persistent variables from '${file.name}'", e)
            return false
        }

        isLoaded = true
        return true
    }

    private fun checkedVersion(json: JSONObject, path: String, supported: Long): Long {
        val versions = json.keySet().filter { LegacyKeys.matches(it, path) }.map { key ->
            val version = json.get(key).toString().toLongOrNull()
                ?: throw IOException("Invalid storage version in '$key'")
            if (version < 0) throw IOException("Negative storage version in '$key'")
            if (version > supported) {
                throw IOException("Storage version $version is newer than supported version $supported")
            }
            version
        }
        if (versions.distinct().size > 1) throw IOException("Conflicting storage version aliases")
        return versions.firstOrNull() ?: 0L
    }

    /**
     * Renames snake_case keys written before v1.22.0 (`version`, `region_groups`, ...) to the
     * current paths, after backing up [file]. Without this, the missing version would make [load]
     * skip every field and the next save would overwrite the old data with empty values.
     *
     * @return `false` if legacy keys were found but the backup could not be written.
     */
    private fun migrateLegacyKeys(file: File, json: JSONObject, versionPath: String?): Boolean {
        val paths = listOfNotNull(versionPath) + persistentFields.mapNotNull { it.path() }
        val renames = paths
            .filter { !json.has(it) }
            .mapNotNull { path -> LegacyKeys.findLegacyKey(json.keySet(), path)?.let { it to path } }
        if (renames.isEmpty()) return true

        try {
            val backup = LegacyKeys.backup(file)
            log.info("Migrating legacy keys in '${file.name}' (backup: '${backup.name}')")
        } catch (e: IOException) {
            log.log(Level.SEVERE, "Not migrating '${file.name}': could not back it up", e)
            return false
        }
        renames.forEach { (old, new) -> json.put(new, json.remove(old)) }
        return true
    }

    /** Saves current persistent data to disk atomically. */
    fun save(file: File) {
        trySave(file)
    }

    /** True only after every field was serialized and the replacement was published. */
    fun trySave(file: File): Boolean {
        if (!isLoaded) return false

        val json = JSONObject()
        val versionPath = requireNotNull(versionPath())
        json.put(versionPath, currentVersion().toString())

        // Skip the whole save if any field fails: writing the rest would drop that field's data from disk.
        try {
            persistentFields.forEach { it.save(json) }
        } catch (e: Exception) {
            log.log(Level.SEVERE, "error while serializing persistent data, keeping the previous '${file.name}'", e)
            return false
        }

        val tmpFile = File("${file.absolutePath}.tmp")
        try {
            Files.writeString(tmpFile.toPath(), json.toString())
        } catch (e: IOException) {
            log.log(Level.SEVERE, "error while saving persistent data to temporary file!", e)
            return false
        }

        return try {
            replaceFile(tmpFile.toPath(), file.toPath())
            true
        } catch (e: IOException) {
            log.log(
                Level.SEVERE,
                "error while atomically replacing '$file' with temporary file (very recent changes might be lost)!", e
            )
            false
        }
    }
}
