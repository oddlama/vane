package org.oddlama.vane.enchantments.enchantments

import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * Data class representing an enchanted item.
 *
 * @property item The [ItemStack] representing the enchanted item.
 * @property level The level of the enchantment.
 */
internal data class EnchantedItem(val item: ItemStack, val level: Int)

/**
 * Extension function for [Player] to get the enchanted chestplate item.
 *
 * @param enchantment The [Enchantment] to look for.
 * @return An instance of [EnchantedItem] representing the enchanted chestplate, or null if none is
 *   found.
 */
internal fun Player.chestplateEnchantment(enchantment: Enchantment?): EnchantedItem? {
    val bukkitEnchantment = enchantment ?: return null
    val chest = equipment.chestplate
    val level = chest.getEnchantmentLevel(bukkitEnchantment)
    if (level == 0) return null
    return EnchantedItem(chest, level)
}
