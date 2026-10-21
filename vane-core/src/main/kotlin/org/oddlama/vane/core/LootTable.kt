package org.oddlama.vane.core

import java.util.*
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Consumer2

/** Runtime loot table extension container for additional generated drops. */
class LootTable {
    /** Additional loot entries grouped by source key. */
    private val possibleLoot = mutableMapOf<NamespacedKey, MutableList<LootTableEntry>>()

    /** Registers a single loot entry for a key. */
    fun put(key: NamespacedKey, entry: LootTableEntry): LootTable = apply {
        possibleLoot[key] = mutableListOf(entry)
    }

    /** Registers multiple loot entries for a key. */
    fun put(key: NamespacedKey, entries: MutableList<LootTableEntry>?): LootTable = apply {
        possibleLoot[key] = entries ?: mutableListOf()
    }

    /** Removes all entries for a key. */
    fun remove(key: NamespacedKey): LootTable = apply { possibleLoot.remove(key) }

    /** Returns grouped possible loot entries. */
    fun possibleLoot(): Map<NamespacedKey, List<LootTableEntry>> = possibleLoot

    /** Returns all entries flattened into a single mutable list. */
    fun flatCopy(): MutableList<LootTableEntry> =
        possibleLoot.values.flatMapTo(mutableListOf()) { it }

    /** Appends generated loot samples into an output list. */
    fun generateLoot(output: MutableList<ItemStack?>?, random: Random) {
        possibleLoot.values.forEach { set ->
            set.forEach { loot ->
                if (loot.evaluateChance(random)) loot.addSample(output, random)
            }
        }
    }

    /** Generates a single override item by weighted chance accumulation. */
    fun generateOverride(random: Random): ItemStack? {
        var totalChance = 0.0
        val threshold = random.nextDouble()
        val resultContainer = mutableListOf<ItemStack?>()
        flatCopy().shuffled(random).forEach { loot ->
            totalChance += loot.chance
            if (totalChance > threshold) loot.addSample(resultContainer, random)
            if (resultContainer.isNotEmpty()) return resultContainer[0]
        }
        return null
    }

    /**
     * Single loot generation entry.
     *
     * @param chance selection chance.
     * @param generator sample generator callback.
     */
    class LootTableEntry(
        val chance: Double,
        val generator: Consumer2<MutableList<ItemStack?>?, Random?>,
    ) {
        /** Creates an entry from expected chest rarity with fixed amount 1. */
        constructor(
            rarityExpectedChests: Int,
            item: ItemStack,
        ) : this(1.0 / rarityExpectedChests, item.clone(), 1, 1)

        /** Creates an entry from expected chest rarity and amount range. */
        constructor(
            rarityExpectedChests: Int,
            item: ItemStack,
            amountMin: Int,
            amountMax: Int,
        ) : this(1.0 / rarityExpectedChests, item.clone(), amountMin, amountMax)

        /** Creates an entry with explicit chance and amount range. */
        constructor(
            chance: Double,
            item: ItemStack,
            amountMin: Int,
            amountMax: Int,
        ) : this(
            chance,
            Consumer2 { list, random ->
                val amount = random!!.nextInt(amountMax - amountMin + 1) + amountMin
                if (amount >= 1) list!!.add(item.clone().also { it.amount = amount })
            },
        )

        /** Adds a sampled item to the output list. */
        fun addSample(items: MutableList<ItemStack?>?, random: Random?) =
            generator.apply(items, random)

        /** Evaluates this entry's random chance. */
        fun evaluateChance(random: Random): Boolean = random.nextDouble() < chance
    }
}
