package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.loot.LootTables
import org.oddlama.vane.annotation.config.ConfigDouble
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

/**
 * The Angel class represents an enchantment that allows players to have enhanced flying abilities.
 *
 * @param context The context in which this enchantment is used.
 * @constructor Creates an instance of the Angel enchantment.
 */
@VaneEnchantment(
    name = "angel",
    maxLevel = 5,
    rarity = Rarity.VERY_RARE,
    treasure = true,
    allowCustom = true,
)
class Angel(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    /**
     * The acceleration percentage configures how much the player's flying speed is increased each
     * tick towards the target speed.
     */
    @ConfigDouble(
        def = 0.1,
        min = 0.0,
        max = 1.0,
        desc =
            "Acceleration percentage. Each tick, the current flying speed is increased X percent towards the target speed. Low values (~0.1) typically result in a smooth acceleration curve and a natural feeling.",
    )
    private val configAccelerationPercentage = 0.0

    /** The flying speed for each enchantment level. */
    @ConfigDoubleList(
        def = [0.7, 1.1, 1.4, 1.7, 2.0],
        min = 0.0,
        desc = "Flying speed in blocks per second for each enchantment level.",
    )
    private val configSpeed: MutableList<Double?>? = null

    /**
     * Defines the default recipes for the Angel enchantment.
     *
     * @return A RecipeList containing the default recipes.
     */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("PRP", "MBM", "MDM")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_the_gods")
                .setIngredient('M', Material.PHANTOM_MEMBRANE)
                .setIngredient('D', Material.DRAGON_BREATH)
                .setIngredient('P', Material.PUFFERFISH_BUCKET)
                .setIngredient('R', Material.FIREWORK_ROCKET)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    /**
     * Defines the default loot tables for the Angel enchantment.
     *
     * @return A LootTableList containing the default loot tables.
     */
    override fun defaultLootTables(): LootTableList {
        return LootTableList.of(
            LootDefinition("generic")
                .`in`(LootTables.BURIED_TREASURE)
                .`in`(LootTables.PILLAGER_OUTPOST)
                .`in`(LootTables.RUINED_PORTAL)
                .`in`(LootTables.STRONGHOLD_LIBRARY)
                .`in`(LootTables.UNDERWATER_RUIN_BIG)
                .`in`(LootTables.VILLAGE_TEMPLE)
                .add(1.0 / 250, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_the_gods"))
        )
    }

    /**
     * Retrieves the flying speed for a given enchantment level.
     *
     * @param level The enchantment level.
     * @return The flying speed in blocks per second.
     */
    private fun speedFor(level: Int): Double {
        val speeds = requireNotNull(configSpeed)
        val index = (level - 1).coerceAtLeast(0)
        return speeds.getOrNull(index) ?: speeds.firstOrNull() ?: 0.0
    }

    /**
     * Event handler for player movement. Adjusts the player's velocity and spawns particles to
     * create the effect of enhanced flying.
     *
     * @param event The PlayerMoveEvent that contains information about the player's movement.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Check sneaking and flying
        val player = event.player
        if (!player.isSneaking || !player.isGliding) {
            return
        }

        // Check enchantment level
        val level = player.chestplateEnchantment(bukkit())?.level ?: return

        val loc = player.location
        val dir = loc.direction
        if (dir.length() == 0.0) {
            return
        }

        // Scale the delta dependent on the angle. Higher angle -> less effect
        val vel = player.velocity
        val delta = configAccelerationPercentage * (1.0 - dir.angle(vel) / Math.PI)
        val factor = speedFor(level)

        // Exponential moving average between velocity and target velocity
        val newVel = vel.multiply(1.0 - delta).add(dir.normalize().multiply(delta * factor))
        player.velocity = newVel

        // Spawn particles
        loc.world.spawnParticle(Particle.FIREWORK, loc, 0, -newVel.x, -newVel.y, -newVel.z, 0.4)
    }
}
