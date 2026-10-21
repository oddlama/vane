package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments
import org.oddlama.vane.util.BlockUtil
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.PlayerUtil

/**
 * Rake is an enchantment that allows players to till additional farmland blocks in a 3x3 area when
 * they right-click on a farmland block with a golden hoe enchanted with this enchantment.
 *
 * @param context The context of the enchantment, providing access to various utility methods and
 *   configurations.
 * @constructor Creates a new Rake enchantment instance.
 */
@VaneEnchantment(name = "rake", maxLevel = 4, rarity = Rarity.COMMON, treasure = true)
class Rake(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    /**
     * Defines the default recipes for the Rake enchantment, allowing players to craft the enchanted
     * item using an ancient tome of knowledge and a golden hoe.
     *
     * @return A RecipeList containing the default crafting recipe for the Rake enchantment.
     */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape(" H ", "HBH", " H ")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('H', Material.GOLDEN_HOE)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    /**
     * Event handler for the PlayerInteractEvent, triggered when a player right-clicks on a block.
     * This method checks if the clicked block is farmland and, if so, tills additional farmland
     * blocks in a 3x3 area around the clicked block, consuming durability from the enchanted golden
     * hoe.
     *
     * @param event The PlayerInteractEvent instance containing information about the event, such as
     *   the clicked block, the player, and the action performed.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerTillFarmland(event: PlayerInteractEvent) {
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val clickedBlock = event.clickedBlock ?: return
        val hand = event.hand ?: return

        // Only till additional blocks when right-clicking farmland
        if (clickedBlock.type != Material.FARMLAND) {
            return
        }

        // Get enchantment level
        val player = event.player
        val item = player.equipment.getItem(hand)
        val level = item.getEnchantmentLevel(requireNotNull(bukkit()))
        if (level == 0) {
            return
        }

        // Get tillable block
        val tillable = BlockUtil.nextTillableBlock(clickedBlock, level, true) ?: return

        // Till block
        if (PlayerUtil.tillBlock(player, tillable)) {
            ItemUtil.damageItem(player, item, 1)
            PlayerUtil.swingArm(player, hand)
        }
    }
}
