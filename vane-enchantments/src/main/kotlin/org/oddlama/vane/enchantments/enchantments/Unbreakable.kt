package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.loot.LootDefinition
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments

/**
 * Unbreakable is a custom enchantment that makes items unbreakable and allows them to bypass normal
 * durability loss. It can be applied to items through crafting or found in loot chests.
 */
@VaneEnchantment(name = "unbreakable", rarity = Rarity.RARE, treasure = true, allowCustom = true)
class Unbreakable(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    /**
     * Defines the default recipes for the Unbreakable enchantment, allowing players to craft
     * enchanted books of the gods using various rare ingredients.
     */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("WAW", "NBN", "TST")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('W', Material.WITHER_ROSE)
                .setIngredient('A', Material.ENCHANTED_GOLDEN_APPLE)
                .setIngredient('N', Material.NETHERITE_INGOT)
                .setIngredient('T', Material.TOTEM_OF_UNDYING)
                .setIngredient('S', Material.NETHER_STAR)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    /**
     * Defines the default loot tables for the Unbreakable enchantment, allowing players to find
     * enchanted books of the gods in various loot chests around the world.
     */
    override fun defaultLootTables(): LootTableList {
        return LootTableList.of(
            LootDefinition("generic")
                .`in`(LootTables.ABANDONED_MINESHAFT)
                .add(1.0 / 120, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods")),
            LootDefinition("bastion")
                .`in`(LootTables.BASTION_TREASURE)
                .add(1.0 / 30, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods")),
        )
    }

    /**
     * Event handler that is called when a player item damage event occurs. If the item has the
     * Unbreakable enchantment, the event will be cancelled and the item will not take durability
     * damage.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onPlayerItemDamage(event: PlayerItemDamageEvent) {
        // Check enchantment
        val item = event.item
        if (item.getEnchantmentLevel(requireNotNull(bukkit())) == 0) {
            return
        }

        // Set item unbreakable to prevent further event calls
        val meta = item.itemMeta
        meta.isUnbreakable = true
        // Also hide the internal unbreakable tag on the client
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        item.itemMeta = meta

        // Prevent damage
        event.damage = 0
        event.isCancelled = true
    }
}
