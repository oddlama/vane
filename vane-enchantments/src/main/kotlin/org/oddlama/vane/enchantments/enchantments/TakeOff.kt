package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.config.ConfigDoubleList
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.loot.LootDefinition
import org.oddlama.vane.core.config.loot.LootTableList
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.PlayerUtil

/**
 * TakeOff is a custom enchantment that provides a boost to players when they use an Elytra, based
 * on the enchantment level. The boost strength increases with each level of the enchantment. This
 * enchantment can be applied to items through crafting or found in loot chests.
 *
 * @param context The context in which this enchantment is used.
 * @constructor Creates a new TakeOff enchantment instance.
 */
@VaneEnchantment(
    name = "take_off",
    maxLevel = 3,
    rarity = Rarity.UNCOMMON,
    treasure = true,
    allowCustom = true,
)
class TakeOff(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    /**
     * Configurable boost strengths for each level of the enchantment. The values are percentages
     * that determine how much the player's speed is boosted when using an Elytra.
     */
    @ConfigDoubleList(
        def = [0.2, 0.4, 0.6],
        min = 0.0,
        desc = "Boost strength for each enchantment level.",
    )
    private val configBoostStrengths: MutableList<Double?>? = null

    /**
     * Defines the default recipes for this enchantment, allowing players to craft the enchanted
     * items.
     *
     * @return A RecipeList containing the default recipes for this enchantment.
     */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("MBM", "PSP")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('M', Material.PHANTOM_MEMBRANE)
                .setIngredient('P', Material.PISTON)
                .setIngredient('S', Material.SLIME_BLOCK)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    /**
     * Defines the default loot tables for this enchantment, determining where players can find the
     * enchanted items as loot.
     *
     * @return A LootTableList containing the default loot tables for this enchantment.
     */
    override fun defaultLootTables(): LootTableList {
        return LootTableList.of(
            LootDefinition("generic")
                .`in`(LootTables.BURIED_TREASURE)
                .`in`(LootTables.PILLAGER_OUTPOST)
                .`in`(LootTables.RUINED_PORTAL)
                .`in`(LootTables.SHIPWRECK_TREASURE)
                .`in`(LootTables.STRONGHOLD_LIBRARY)
                .`in`(LootTables.UNDERWATER_RUIN_BIG)
                .`in`(LootTables.UNDERWATER_RUIN_SMALL)
                .`in`(LootTables.VILLAGE_TEMPLE)
                .`in`(LootTables.WOODLAND_MANSION)
                .add(1.0 / 150, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    /**
     * Retrieves the boost strength for a given enchantment level.
     *
     * @param level The enchantment level.
     * @return The boost strength corresponding to the given level.
     */
    private fun getBoostStrength(level: Int): Double {
        val strengths = requireNotNull(configBoostStrengths)
        val index = (level - 1).coerceAtLeast(0)
        return strengths.getOrNull(index) ?: strengths.firstOrNull() ?: 0.0
    }

    /**
     * Event handler that is called when a player toggles glide. Applies the Elytra boost to the
     * player if they have the enchantment.
     *
     * @param event The EntityToggleGlideEvent that contains information about the toggle glide
     *   action.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerToggleGlide(event: EntityToggleGlideEvent) {
        val player = event.entity as? Player ?: return
        if (!event.isGliding) {
            return
        }

        // Don't apply for sneaking players
        if (player.isSneaking) {
            return
        }

        // Check enchantment level
        val enchantedChest = player.chestplateEnchantment(bukkit()) ?: return
        val chest = enchantedChest.item
        val level = enchantedChest.level

        // Apply boost
        PlayerUtil.applyElytraBoost(player, getBoostStrength(level))
        ItemUtil.damageItem(player, chest, (1.0 + 2.0 * Math.random()).toInt())

        // Spawn particles
        PlayerUtil.spawnElytraBoostParticles(player)
    }
}
