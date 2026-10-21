package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.block.data.type.Leaves
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments

/**
 * Leafchopper is a custom enchantment that allows players to instantly break non-persistent leaves
 * blocks without any additional durability cost to the tool, when the player left-clicks on the
 * leaves with the tool in hand.
 *
 * @param context The context of the enchantment, providing access to the Enchantments instance and
 *   other necessary data.
 * @constructor Creates a new instance of the Leafchopper enchantment.
 */
@VaneEnchantment(name = "leafchopper", rarity = Rarity.COMMON, treasure = true)
class Leafchopper(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    /**
     * Defines the default recipes for the enchantment, allowing it to be crafted or obtained
     * through specific crafting recipes.
     *
     * @return A RecipeList containing the default recipes for the enchantment.
     */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape(" S ", "SBS", " S ")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('S', Material.SHEARS)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    /**
     * Event handler that is triggered when a player left-clicks on a block. If the block is a
     * non-persistent leaves block, it will be broken instantly, and a breaking sound will be
     * played.
     *
     * @param event The PlayerInteractEvent that contains information about the player, the block
     *   clicked, and other relevant data.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerLeftClickLeaves(event: PlayerInteractEvent) {
        if (
            !event.hasBlock() ||
                event.hand != EquipmentSlot.HAND ||
                event.action != Action.LEFT_CLICK_BLOCK
        ) {
            return
        }

        // Check leaves
        val block = event.clickedBlock ?: return
        val data = block.blockData
        if (data !is Leaves) {
            return
        }

        // Check non-persistent leaves
        val leaves = data
        if (leaves.isPersistent) {
            return
        }

        // Check enchantment level
        val player = event.player
        val item = player.equipment.itemInMainHand
        val level = item.getEnchantmentLevel(requireNotNull(bukkit()))
        if (level == 0) {
            return
        }

        // Break instantly, for no additional durability cost.
        block.breakNaturally()
        block.world.playSound(
            block.location,
            Sound.BLOCK_GRASS_BREAK,
            SoundCategory.BLOCKS,
            1.0f,
            1.0f,
        )
    }
}
