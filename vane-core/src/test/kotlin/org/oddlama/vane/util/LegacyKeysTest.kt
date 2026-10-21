package org.oddlama.vane.util

import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.bukkit.configuration.file.YamlConfiguration
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer

class LegacyKeysTest {
    @Test
    fun `finds snake_case spelling of a PascalCase key`() {
        val keys = listOf("version", "resource_pack", "lang")
        assertEquals("resource_pack", LegacyKeys.findLegacyKey(keys, "ResourcePack"))
        assertEquals("version", LegacyKeys.findLegacyKey(keys, "Version"))
        assertNull(LegacyKeys.findLegacyKey(keys, "Metrics"))
    }

    @Test
    fun `matches acronyms and dotted storage paths`() {
        assertEquals("motd", LegacyKeys.findLegacyKey(listOf("motd"), "MOTD"))
        assertEquals(
            "auth_multiplexer.auth_multiplex",
            LegacyKeys.findLegacyKey(
                listOf("auth_multiplexer.auth_multiplex"),
                "AuthMultiplexer.AuthMultiplex",
            ),
        )
    }

    @Test
    fun `migrates a pre-1_22 config to the current paths`() {
        val legacy =
            YamlConfiguration().apply {
                loadFromString(
                    """
                    version: 6
                    lang: de
                    resource_pack:
                      enabled: true
                      force: false
                      custom_resource_pack:
                        enabled: true
                        url: https://example.org/pack.zip
                    command_vane:
                      enabled: false
                    enchantment_take_off:
                      enabled: true
                      level_cost:
                        diamond_pickaxe: 3
                        netherite_pickaxe: 5
                    unrelated_old_key: 1
                    """
                        .trimIndent()
                )
            }

        val migrated =
            LegacyKeys.migrateYaml(
                legacy,
                listOf(
                    "Version",
                    "Lang",
                    "ResourcePack.Enabled",
                    "ResourcePack.Force",
                    "ResourcePack.CustomResourcePack.Enabled",
                    "ResourcePack.CustomResourcePack.Url",
                    "CommandVane.Enabled",
                    "EnchantmentTakeOff.LevelCost",
                    "Metrics.Enabled",
                ),
            )

        assertEquals(6L, migrated.getLong("Version"))
        assertEquals("de", migrated.getString("Lang"))
        assertTrue(migrated.getBoolean("ResourcePack.Enabled"))
        assertFalse(migrated.getBoolean("ResourcePack.Force", true))
        assertEquals(
            "https://example.org/pack.zip",
            migrated.getString("ResourcePack.CustomResourcePack.Url"),
        )
        assertFalse(migrated.getBoolean("CommandVane.Enabled", true))
        // A map value becomes a section again, and its user-defined keys are not renamed.
        val levelCost = migrated.getConfigurationSection("EnchantmentTakeOff.LevelCost")!!
        assertEquals(setOf("diamond_pickaxe", "netherite_pickaxe"), levelCost.getKeys(false))
        assertEquals(5, levelCost.getInt("netherite_pickaxe"))
        // Fields missing from the old file keep their defaults, and unknown keys are dropped.
        assertFalse(migrated.contains("Metrics.Enabled"))
        assertFalse(migrated.contains("unrelated_old_key"))
    }

    @Test
    fun `looks up dictionary keys in any spelling`() {
        val legacy = mapOf("copy_nbt" to true, "cooking_time" to 200)
        assertEquals(true, LegacyKeys.lookup(legacy, "CopyNbt"))
        assertEquals(200, LegacyKeys.lookup(legacy, "CookingTime"))
        assertNull(LegacyKeys.lookup(legacy, "Result"))
        // The current spelling wins over a legacy one.
        assertEquals(1, LegacyKeys.lookup(mapOf("Chance" to 1, "chance" to 2), "Chance"))
    }

    @Test
    fun `matches portal style names in any spelling`() {
        assertTrue(LegacyKeys.matches("boundary_1", "BOUNDARY1"))
        assertTrue(LegacyKeys.matches("active", "Active"))
        assertFalse(LegacyKeys.matches("inactive", "Active"))
    }

    @Test
    fun `renames legacy keys in serialized objects`() {
        val json = JSONObject("""{"target_id": "a", "target_locked": true, "name": "Home"}""")
        LegacyKeys.upgradeJsonObject(json)
        assertEquals(setOf("targetId", "targetLocked", "name"), json.keySet())
        assertEquals("a", json.getString("targetId"))
    }

    @Test
    fun `keeps the current key when both spellings exist`() {
        val json = JSONObject("""{"world_id": "old", "worldId": "new"}""")
        LegacyKeys.upgradeJsonObject(json)
        assertEquals("new", json.getString("worldId"))
    }

    @Test
    fun `deserializes a location saved before 1_22`() {
        val world = UUID.randomUUID()
        // Numbers are stored as strings, as PersistentSerializer writes them.
        val json = JSONObject("""{"world_id": "$world", "x": "1", "y": "64", "z": "-3"}""")
        val block = PersistentSerializer.fromJson(LazyBlock::class.java, json)!!
        assertEquals(world, block.worldId)
        assertEquals(64, block.y)
    }

    @Test
    fun `backup copies the file once`() {
        val dir = Files.createTempDirectory("vane-legacy").toFile()
        try {
            val file = File(dir, "config.yml").apply { writeText("version: 6\n") }
            val backup = LegacyKeys.backup(file)
            assertEquals("config.yml.pre-1.22.bak", backup.name)
            file.writeText("Version: 6\n")
            LegacyKeys.backup(file)
            assertEquals("version: 6\n", backup.readText())
        } finally {
            dir.deleteRecursively()
        }
    }
}
