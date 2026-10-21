package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.enchantment.Rarity
import org.oddlama.vane.annotation.enchantment.VaneEnchantment
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.enchantments.CustomEnchantment
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.enchantments.Enchantments

/**
 * The [Lightning] class represents a lightning enchantment that can be applied to weapons.
 *
 * @param context The context in which this enchantment is used.
 * @constructor Creates a new instance of the [Lightning] enchantment.
 */
@VaneEnchantment(name = "lightning", maxLevel = 1, rarity = Rarity.RARE, treasure = true)
class Lightning(context: Context<Enchantments?>) :
    CustomEnchantment<Enchantments?>(context, false) {
    /** Configurable option to toggle lightning protection. */
    @ConfigBoolean(
        def = true,
        desc =
            "Toggle lightning enchantment to cancel lightning damage for wielders of the enchant",
    )
    private val configLightningProtection = false

    /** Configurable damage modifier for the lightning enchantment. */
    @ConfigInt(def = 4, min = 0, max = 20, desc = "Damage modifier for the lightning enchant")
    private val configLightningDamage = 0

    /** Configurable option to enable lightning to work in rainstorms. */
    @ConfigBoolean(def = true, desc = "Enable lightning to work in rainstorms as well")
    private val configLightningRain = false

    /**
     * Defines the default recipes for the lightning enchantment.
     *
     * @return A [RecipeList] containing the default recipes.
     */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("R R", "UTU", " B ")
                .setIngredient('R', Material.LIGHTNING_ROD)
                .setIngredient('T', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('B', Material.BEACON)
                .setIngredient('U', Material.TOTEM_OF_UNDYING)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    /**
     * Event handler for when a player with the lightning enchantment is attacked by lightning.
     *
     * @param event The [EntityDamageEvent] containing the event details.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onLightningAttack(event: EntityDamageEvent) {
        // Check if an entity is a player
        val player = event.entity as? Player ?: return

        // Check to see if they were struck by lightning
        if (event.cause != DamageCause.LIGHTNING) return

        // Check to see if lightning protection is off
        if (!configLightningProtection) return

        val item = player.equipment.itemInMainHand
        val level = item.getEnchantmentLevel(requireNotNull(bukkit()))

        // If they are not holding a lightning sword, they still take the damage
        if (level == 0) return

        // Cancel the damage to the event
        event.isCancelled = true
    }

    /**
     * Event handler for when a player attacks another entity with a weapon imbued with the
     * lightning enchantment.
     *
     * @param event The [EntityDamageByEntityEvent] containing the event details.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onSwordAttack(event: EntityDamageByEntityEvent) {
        // Only strike when an entity is a player
        if (event.damager !is Player) return

        // if not an attack with a weapon exit
        if (event.cause != DamageCause.ENTITY_ATTACK) return

        val damager = event.damager as Player
        val damagee = event.entity
        val world = damager.world
        val item = damager.equipment.itemInMainHand
        val level = item.getEnchantmentLevel(requireNotNull(bukkit()))

        // Get enchantment level
        if (level == 0) return

        // Get Storm status
        if (!world.hasStorm()) return

        // Exit if config set to thunder only
        if (!configLightningRain && !world.isThundering) return

        // Test if sky is visible
        if (damagee.location.blockY < world.getHighestBlockYAt(damagee.location)) return

        // Execute
        event.damage += configLightningDamage
        world.strikeLightning(damagee.location)
    }
}
