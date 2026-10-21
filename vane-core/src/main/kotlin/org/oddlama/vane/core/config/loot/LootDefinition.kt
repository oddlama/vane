package org.oddlama.vane.core.config.loot

import org.bukkit.NamespacedKey
import org.bukkit.loot.LootTables
import org.oddlama.vane.core.LootTable
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.LegacyKeys
import org.oddlama.vane.util.StorageUtil.namespacedKey

/**
 * Config definition for additional entries injected into vanilla loot tables.
 *
 * @param name logical loot-definition name.
 */
class LootDefinition(val name: String?) {
    /** Internal serializable loot entry representation. */
    private data class Entry(
        /** Drop chance in percent-like units used by vane loot table processing. */
        val chance: Double,
        /** Minimum amount for generated stack size. */
        val amountMin: Int,
        /** Maximum amount for generated stack size. */
        val amountMax: Int,
        /** Item definition string parsed via [ItemUtil.itemstackFromString]. */
        val itemDefinition: String?,
    ) {
        /** Serializes this entry to dictionary form. */
        fun serialize(): Map<String, Any?> =
            mapOf(
                "Chance" to chance,
                "AmountMin" to amountMin,
                "AmountMax" to amountMax,
                "Item" to itemDefinition,
            )

        /** Entry deserialization helpers. */
        companion object {
            /** Deserializes an [Entry] from map data. */
            fun deserialize(map: Map<String?, Any?>): Entry {
                val chance =
                    when (val v = LegacyKeys.lookup(map, "Chance")) {
                        is Double -> v
                        is Float -> v.toDouble()
                        is Int -> v.toDouble()
                        is Long -> v.toDouble()
                        else ->
                            throw IllegalArgumentException(
                                "Invalid loot table entry: chance must be a number (double)!"
                            )
                    }
                val amountMin =
                    when (val v = LegacyKeys.lookup(map, "AmountMin")) {
                        is Int -> v
                        is Double -> v.toInt()
                        is Long -> v.toInt()
                        else ->
                            throw IllegalArgumentException(
                                "Invalid loot table entry: amountMin must be an int!"
                            )
                    }
                val amountMax =
                    when (val v = LegacyKeys.lookup(map, "AmountMax")) {
                        is Int -> v
                        is Double -> v.toInt()
                        is Long -> v.toInt()
                        else ->
                            throw IllegalArgumentException(
                                "Invalid loot table entry: amountMax must be an int!"
                            )
                    }
                val itemDefinition = LegacyKeys.lookup(map, "Item")?.toString()
                return Entry(chance, amountMin, amountMax, itemDefinition)
            }
        }
    }

    /** Loot tables affected by this definition. */
    val affectedTables: MutableList<NamespacedKey?> = mutableListOf()

    /** Serialized loot entries belonging to this definition. */
    private val entries: MutableList<Entry> = mutableListOf()

    /** Builds a unique key for this definition under a base key. */
    fun key(baseKey: NamespacedKey): NamespacedKey =
        namespacedKey(baseKey.namespace(), "${baseKey.value()}.$name")

    /** Adds an affected loot table by namespaced key. */
    fun `in`(table: NamespacedKey?): LootDefinition = apply { affectedTables.add(table) }

    /** Adds an affected loot table from Bukkit [LootTables]. */
    fun `in`(table: LootTables): LootDefinition = `in`(table.key)

    /** Adds a prebuilt entry. */
    private fun add(entry: Entry): LootDefinition = apply { entries.add(entry) }

    /** Adds a loot entry from primitives and item definition string. */
    fun add(
        chance: Double,
        amountMin: Int,
        amountMax: Int,
        itemDefinition: String?,
    ): LootDefinition = add(Entry(chance, amountMin, amountMax, itemDefinition))

    /** Serializes this loot definition to dictionary form. */
    fun serialize(): Map<String, Any> =
        mapOf(
            "Tables" to affectedTables.map { it.toString() },
            "Items" to entries.map { it.serialize() },
        )

    /** Converts serialized entries into runtime loot-table entries. */
    fun entries(): List<LootTable.LootTableEntry> = entries.map { e ->
        LootTable.LootTableEntry(
            e.chance,
            ItemUtil.itemstackFromString(e.itemDefinition!!).first!!,
            e.amountMin,
            e.amountMax,
        )
    }

    /** Deserialization helpers. */
    companion object {
        /** Deserializes a [LootDefinition] from dictionary form. */
        fun deserialize(name: String?, rawDict: Any): LootDefinition {
            require(rawDict is Map<*, *>) {
                "Invalid loot table: Argument must be a Map<String, Object>, but is ${rawDict.javaClass}!"
            }

            val tableDict =
                rawDict.entries
                    .filter { it.key is String }
                    .associate { it.key as String to it.value }

            val tablesObj = LegacyKeys.lookup(tableDict, "Tables")
            require(tablesObj is List<*>) { "Invalid loot table: 'tables' must be a list" }

            val itemsObj = LegacyKeys.lookup(tableDict, "Items")
            require(itemsObj is List<*>) { "Invalid loot table: 'items' must be a list" }

            return LootDefinition(name).also { table ->
                tablesObj.filterIsInstance<String>().forEach {
                    table.`in`(NamespacedKey.fromString(it))
                }
                itemsObj.filterIsInstance<Map<*, *>>().forEach { e ->
                    val mapped =
                        e.entries
                            .filter { it.key is String }
                            .associate { it.key as String? to it.value }
                    table.add(Entry.deserialize(mapped))
                }
            }
        }
    }
}
