package org.oddlama.vane.util

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.json.JSONObject

/**
 * Helpers for reading data written by vane releases before v1.22.0.
 *
 * Those releases used snake_case keys (`resource_pack.force`, `world_id`), which the Kotlin rewrite
 * renamed to PascalCase or camelCase (`ResourcePack.Force`, `worldId`). Every rename only changed
 * casing and underscores, so an old key is found by comparing [normalize]d names.
 */
object LegacyKeys {
    /**
     * Keys inside serialized objects (portals, regions, region groups, roles, locations) that were
     * renamed. These objects live in world data and `storage.json`, so they cannot be matched by a
     * known field path like config and storage keys are.
     */
    private val JSON_RENAMES =
        mapOf(
            "active_materials" to "activeMaterials",
            "exit_orientation_locked" to "exitOrientationLocked",
            "inactive_materials" to "inactiveMaterials",
            "player_to_role" to "playerToRole",
            "portal_id" to "portalId",
            "region_group" to "regionGroup",
            "role_others" to "roleOthers",
            "role_type" to "roleType",
            "style_override" to "styleOverride",
            "target_id" to "targetId",
            "target_locked" to "targetLocked",
            "world_id" to "worldId",
        )

    /**
     * Returns [key] without underscores and in lower case, so that `resource_pack`, `ResourcePack`
     * and `resourcePack` compare equal. Dots are kept, so dotted paths still compare component by
     * component.
     */
    fun normalize(key: String): String = key.replace("_", "").lowercase()

    /**
     * Returns the key in [keys] that is a legacy spelling of [name], or `null` if there is none.
     */
    fun findLegacyKey(keys: Iterable<String>, name: String): String? {
        val wanted = normalize(name)
        return keys.firstOrNull { it != name && normalize(it) == wanted }
    }

    /**
     * Returns the value of [key] in [map], or of its legacy spelling (`copy_nbt` for `CopyNbt`).
     * Used for keys inside config dictionaries (recipes, loot tables), which are user data and so
     * are not renamed when a config is migrated.
     */
    fun lookup(map: Map<*, *>, key: String): Any? {
        if (map.containsKey(key)) return map[key]
        val legacy = findLegacyKey(map.keys.filterIsInstance<String>(), key) ?: return null
        return map[legacy]
    }

    /** Returns whether [value] is [name] in any spelling (`boundary_1` for `Boundary1`). */
    fun matches(value: String, name: String): Boolean = normalize(value) == normalize(name)

    /**
     * Renames legacy keys of a serialized object in place. Nested objects are handled when they are
     * deserialized themselves. A key is left alone if its new name is already present.
     */
    fun upgradeJsonObject(json: JSONObject) {
        for ((old, new) in JSON_RENAMES) {
            if (json.has(old) && !json.has(new)) {
                json.put(new, json.remove(old))
            }
        }
    }

    /**
     * Builds a config holding the values of [paths] found in the legacy config [legacy]. Only the
     * values of known fields are carried over, so user-defined keys inside them (material names,
     * recipe ids, ...) are kept as they are.
     */
    fun migrateYaml(legacy: ConfigurationSection, paths: Iterable<String>): YamlConfiguration {
        val migrated = YamlConfiguration()
        for (path in paths) {
            val value = findLegacyValue(legacy, path) ?: continue
            migrated.set(path, toPlainValue(value))
        }
        // set() stores maps as-is; reparsing turns them into sections like a loaded file has.
        return YamlConfiguration().apply { loadFromString(migrated.saveToString()) }
    }

    /** Looks up [path] in [section], matching each component by its [normalize]d name. */
    private fun findLegacyValue(section: ConfigurationSection, path: String): Any? {
        var current = section
        val components = path.split(".")
        for ((i, component) in components.withIndex()) {
            val keys = current.getKeys(false)
            val key =
                component.takeIf { it in keys } ?: findLegacyKey(keys, component) ?: return null
            if (i == components.lastIndex) return current.get(key)
            current = current.getConfigurationSection(key) ?: return null
        }
        return null
    }

    /**
     * Turns [ConfigurationSection]s into plain maps, so that they can be written under a new path.
     */
    private fun toPlainValue(value: Any?): Any? =
        when (value) {
            is ConfigurationSection ->
                value.getKeys(false).associateWith { toPlainValue(value.get(it)) }
            else -> value
        }

    /**
     * Copies [file] to `<name>.pre-1.22.bak` next to it, unless such a backup already exists.
     *
     * @return the backup file.
     */
    @Throws(IOException::class)
    fun backup(file: File): File {
        val backup = File(file.parentFile, "${file.name}.pre-1.22.bak")
        if (!backup.exists()) {
            Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        }
        return backup
    }
}
