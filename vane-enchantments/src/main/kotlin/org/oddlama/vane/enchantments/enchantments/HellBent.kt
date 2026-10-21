package org.oddlama.vane.enchantments.enchantments

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
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
 * Prevents damage from the FLY_INTO_WALL cause when a player is gliding with an Elytra and is
 * wearing a helmet enchanted with HellBent. When the conditions are met (player is a gliding
 * player, struck a wall, and the helmet has this enchantment), the damaging event is cancelled so
 * the player does not take collision damage.
 *
 * @param context The context for the enchantment.
 * @constructor Creates a new instance of the HellBent enchantment.
 */
@VaneEnchantment(name = "hell_bent", rarity = Rarity.COMMON, treasure = true)
class HellBent(context: Context<Enchantments?>) : CustomEnchantment<Enchantments?>(context) {
    /**
     * Defines the default recipes for the HellBent enchantment.
     *
     * @return A [RecipeList] containing the default recipes.
     */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("M", "B", "T")
                .setIngredient('B', "vane_enchantments:ancient_tome_of_knowledge")
                .setIngredient('T', Material.TURTLE_HELMET)
                .setIngredient('M', Material.MUSIC_DISC_PIGSTEP)
                .result(on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    /**
     * Defines the default loot tables for the HellBent enchantment.
     *
     * @return A [LootTableList] containing the default loot tables.
     */
    override fun defaultLootTables(): LootTableList {
        return LootTableList.of(
            LootDefinition("generic")
                .`in`(LootTables.BASTION_BRIDGE)
                .`in`(LootTables.BASTION_HOGLIN_STABLE)
                .`in`(LootTables.BASTION_OTHER)
                .`in`(LootTables.BASTION_TREASURE)
                .add(1.0 / 50, 1, 1, on("vane_enchantments:enchanted_ancient_tome_of_knowledge"))
        )
    }

    /**
     * Cancels damage events caused by flying into a wall while gliding with an Elytra when the
     * player is wearing a helmet that has the HellBent enchantment. Listens for [EntityDamageEvent]
     * and cancels it when the attack matches [DamageCause.FLY_INTO_WALL] and the helmet enchants
     * are present.
     *
     * @param event The [EntityDamageEvent] to inspect and possibly cancel.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity !is Player || event.cause != DamageCause.FLY_INTO_WALL) {
            return
        }

        // Get helmet
        val player = entity
        val helmet = player.equipment.helmet

        // Check enchantment
        if (helmet.getEnchantmentLevel(requireNotNull(bukkit())) == 0) {
            return
        }

        event.isCancelled = true
    }
}
