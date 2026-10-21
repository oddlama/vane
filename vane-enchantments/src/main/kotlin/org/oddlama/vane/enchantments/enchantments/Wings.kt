package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.config.ConfigDoubleList
import org.oddlama.vane.annotation.config.ConfigIntList
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
import org.oddlama.vane.util.msToTicks

/**
 * Wings enchantment class that provides elytra flying boosts.
 *
 * @param context The context for the enchantment.
 * @constructor Creates a new Wings enchantment instance.
 */
@VaneEnchantment(
    name = "wings",
    maxLevel = 4,
    rarity = Rarity.RARE,
    treasure = true,
    allowCustom = true,
)
class Wings(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    /** Cooldown boost configuration in milliseconds for each enchantment level. */
    @ConfigIntList(
        def = [7000, 5000, 3500, 2800],
        min = 0,
        desc = "Boost cooldown in milliseconds for each enchantment level.",
    )
    private val configBoostCooldowns: MutableList<Int?>? = null

    /** Strength boost configuration for each enchantment level. */
    @ConfigDoubleList(
        def = [0.4, 0.47, 0.54, 0.6],
        min = 0.0,
        desc = "Boost strength for each enchantment level.",
    )
    private val configBoostStrengths: MutableList<Double?>? = null

    /**
     * Defines the default recipes for the Wings enchantment.
     *
     * @return A list of default recipes.
     */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("M M", "DBD", "R R")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('M', Material.PHANTOM_MEMBRANE)
                .setIngredient('D', Material.DISPENSER)
                .setIngredient('R', Material.FIREWORK_ROCKET)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    /**
     * Defines the default loot tables for the Wings enchantment.
     *
     * @return A list of default loot tables.
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
                .add(1.0 / 110, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_knowledge")),
            LootDefinition("bastion")
                .`in`(LootTables.BASTION_TREASURE)
                .add(1.0 / 10, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_knowledge")),
        )
    }

    /**
     * Gets the cooldown boost for a given enchantment level.
     *
     * @param level The enchantment level.
     * @return The cooldown boost in ticks.
     */
    private fun getBoostCooldown(level: Int): Int {
        val cooldowns = requireNotNull(configBoostCooldowns)
        val index = (level - 1).coerceAtLeast(0)
        return cooldowns.getOrNull(index) ?: cooldowns.firstOrNull() ?: 0
    }

    /**
     * Gets the strength boost for a given enchantment level.
     *
     * @param level The enchantment level.
     * @return The strength boost.
     */
    private fun getBoostStrength(level: Int): Double {
        val strengths = requireNotNull(configBoostStrengths)
        val index = (level - 1).coerceAtLeast(0)
        return strengths.getOrNull(index) ?: strengths.firstOrNull() ?: 0.0
    }

    /**
     * Event handler for player sneak toggling. Applies the elytra boost if conditions are met.
     *
     * @param event The player sneak event.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        // Check sneaking and flying
        val player = event.player
        if (!event.isSneaking || !player.isGliding) {
            return
        }

        // Check enchantment level
        val enchantedChest = player.chestplateEnchantment(bukkit()) ?: return
        val chest = enchantedChest.item
        val level = enchantedChest.level

        // Check cooldown
        if (player.getCooldown(Material.ELYTRA) > 0) {
            return
        }

        // Apply boost
        val cooldown = msToTicks(getBoostCooldown(level).toLong())
        player.setCooldown(Material.ELYTRA, cooldown.toInt())
        PlayerUtil.applyElytraBoost(player, getBoostStrength(level))
        ItemUtil.damageItem(player, chest, (1.0 + 2.0 * Math.random()).toInt())

        // Spawn particles
        PlayerUtil.spawnElytraBoostParticles(player)
    }
}
