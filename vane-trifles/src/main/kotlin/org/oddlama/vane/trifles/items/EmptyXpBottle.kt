package org.oddlama.vane.trifles.items

import java.util.*
import kotlin.math.roundToInt
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapelessRecipeDefinition
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.trifles.items.XpBottles.XpBottle
import org.oddlama.vane.util.PlayerUtil
import org.oddlama.vane.util.expForLevel

@VaneItem(name = "empty_xp_bottle", base = Material.GLASS_BOTTLE, modelData = 0x76000a, version = 1)
/** Consumes player experience to create the largest possible filled XP bottle variant. */
class EmptyXpBottle(context: Context<Trifles?>) : CustomItem<Trifles?>(context) {
    /** Percentage of XP lost while converting to a filled bottle. */
    @ConfigDouble(
        def = 0.3,
        min = 0.0,
        max = 0.999,
        desc =
            "Percentage of experience lost when bottling. For 10% loss, bottling 30 levels will require 30 * (1 / (1 - 0.1)) = 33.33 levels",
    )
    /** Percentage of XP lost while converting to a filled bottle. */
    var configLossPercentage: Double = 0.0

    /** Defines the empty XP bottle crafting recipe. */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapelessRecipeDefinition("generic")
                .addIngredient(Material.EXPERIENCE_BOTTLE)
                .addIngredient(Material.GLASS_BOTTLE)
                .addIngredient(Material.GOLD_NUGGET)
                .result(key().toString())
        )
    }

    /** Prevents vanilla interactions that conflict with custom bottling behavior. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(
            InhibitBehavior.USE_IN_VANILLA_RECIPE,
            InhibitBehavior.TEMPT,
            InhibitBehavior.USE_OFFHAND,
        )
    }

    @EventHandler(
        priority = EventPriority.NORMAL,
        ignoreCancelled = false,
    ) // Keep disabled-cancel filtering to include right-click-air usage.
    /** Converts player experience into a matching filled XP bottle item. */
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) {
            return
        }

        // Require the empty XP bottle custom item.
        val player = event.player
        val hand = event.hand ?: return
        val item = player.equipment.getItem(hand)
        if (!isInstance(item)) {
            return
        }

        // Block vanilla usage of the underlying glass bottle item.
        event.setUseItemInHand(Event.Result.DENY)

        when (event.action) {
            Action.RIGHT_CLICK_AIR -> {}
            Action.RIGHT_CLICK_BLOCK ->
                // Trigger only when block interaction was denied by prior handlers.
                if (event.useInteractedBlock() != Event.Result.DENY) {
                    return
                }

            else -> return
        }

        // Guard against accidental immediate refill after consuming an XP bottle.
        val now = System.currentTimeMillis()
        val module = module ?: return
        val lastConsume = module.lastXpBottleConsumeTime.getOrDefault(player.uniqueId, 0L)
        if (now - lastConsume < 1000) {
            return
        }

        // Pick the largest bottle the player can currently afford.
        var xpBottle: XpBottle? = null
        var exp = 0
        val xpBottles = module.xpBottles ?: return
        for (bottle in xpBottles.bottles) {
            val curExp =
                ((1.0 / (1.0 - configLossPercentage)) * expForLevel(bottle.configCapacity)).toInt()

            // Prefer highest-capacity affordable variant.
            if (getTotalExp(player) >= curExp && curExp > exp) {
                exp = curExp
                xpBottle = bottle
            }
        }

        // Abort when no variant can be filled from available XP.
        if (xpBottle == null) {
            return
        }

        // Deduct XP, consume source item, and grant selected filled bottle.
        player.giveExp(-exp, false)
        PlayerUtil.removeOneItemFromHand(player, hand)
        PlayerUtil.giveItem(player, xpBottle.newStack())
        player.world.playSound(
            player.location,
            Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
            SoundCategory.PLAYERS,
            1.0f,
            4.0f,
        )
        PlayerUtil.swingArm(player, hand)
    }

    companion object {
        /** Calculates absolute experience total from level and fractional progress. */
        fun getTotalExp(player: Player): Int {
            // Convert fractional bar progress into integer experience points.
            return levelToExp(player.level) + (player.expToLevel * player.exp).roundToInt()
        }

        /** Converts a level to total cumulative experience points. */
        fun levelToExp(level: Int): Int {
            // Vanilla XP formulas: https://minecraft.fandom.com/wiki/Experience#Leveling_up
            if (level > 30) {
                return (4.5 * level * level - 162.5 * level + 2220).toInt()
            }
            if (level > 15) {
                return (2.5 * level * level - 40.5 * level + 360).toInt()
            }
            return level * level + 6 * level
        }
    }
}
