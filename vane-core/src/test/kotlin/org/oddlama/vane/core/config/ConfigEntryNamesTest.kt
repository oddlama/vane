package org.oddlama.vane.core.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList

class ConfigEntryNamesTest {
    private val names =
        ConfigEntryNames(mapOf("generic" to "Generic", "terralith_generic" to "TerralithGeneric"))

    @Test
    fun `maps default entries both ways`() {
        assertEquals("TerralithGeneric", names.toYaml("terralith_generic"))
        assertEquals("terralith_generic", names.fromYaml("TerralithGeneric"))
        // Configs from before v1.22.0 use the internal name as the key.
        assertEquals("terralith_generic", names.fromYaml("terralith_generic"))
    }

    @Test
    fun `keeps administrator ids exactly`() {
        for (id in
            listOf(
                "terralith__generic",
                "Terralith_Generic",
                "TERRALITH_GENERIC",
                "terralith generic",
                "GENERIC",
                "my_recipe",
            )) {
            assertEquals(id, names.fromYaml(id))
            assertEquals(id, names.toYaml(id))
        }
    }

    @Test
    fun `rejects two keys for the same entry`() {
        assertEquals(
            listOf("generic", "terralith__generic"),
            names.fromYaml(listOf("Generic", "terralith__generic"), "recipes"),
        )
        assertFailsWith<YamlLoadException> {
            names.fromYaml(listOf("Generic", "generic"), "recipes")
        }
    }

    private fun stonecutting() =
        mutableMapOf<String, Any>(
            "Type" to "stonecutting",
            "Input" to "minecraft:stone",
            "Result" to "minecraft:stone_slab",
        )

    @Test
    fun `recipe ids survive repeated loads and saves`() {
        var dict =
            mutableMapOf<String, Any>(
                "Generic" to stonecutting(),
                "terralith_generic" to stonecutting(),
                "terralith__generic" to stonecutting(),
                "Terralith_Generic" to stonecutting(),
            )
        repeat(3) {
            dict = RecipeList().apply { fromDict(dict) }.toDict()
        }
        assertEquals(
            setOf("Generic", "TerralithGeneric", "terralith__generic", "Terralith_Generic"),
            dict.keys,
        )
        assertEquals(
            listOf("generic", "terralith_generic", "terralith__generic", "Terralith_Generic"),
            RecipeList().apply { fromDict(dict) }.recipes().map { it!!.name },
        )
    }

    @Test
    fun `loot ids survive repeated loads and saves`() {
        fun loot() =
            mutableMapOf<String, Any>("Tables" to listOf<String>(), "Items" to listOf<Any>())
        var dict =
            mutableMapOf<String, Any>(
                "AncientCity" to loot(),
                "ancient_city" to loot(),
                "my__loot" to loot(),
            )
        repeat(3) {
            dict = LootTableList().apply { fromDict(dict) }.toDict()
        }
        assertEquals(setOf("AncientCity", "ancient_city", "my__loot"), dict.keys)
    }
}
