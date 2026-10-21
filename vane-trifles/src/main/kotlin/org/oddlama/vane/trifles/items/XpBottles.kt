package org.oddlama.vane.trifles.items

import java.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.EquipmentSlot
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.CustomItemHelper
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.PlayerUtil
import org.oddlama.vane.util.expForLevel

/** Registers custom XP bottle items and handles their consumption behavior. */
class XpBottles(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group("XpBottles", "Several XP bottles storing a certain amount of experience.")
    ) {
    /** Base type for XP bottle variants with configurable level capacity. */
    abstract class XpBottle(context: Context<Trifles?>) : CustomItem<Trifles?>(context) {
        /** Maximum level-equivalent amount restored by this bottle. */
        @JvmField @ConfigInt(def = -1, desc = "Level capacity.") var configCapacity: Int = 0

        /** Colors bottle display names for easier identification. */
        override fun displayName(): Component? {
            return super.displayName()?.color(NamedTextColor.YELLOW)
        }

        /** Prevents XP bottles from interacting with unsupported vanilla behaviors. */
        override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
            return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND)
        }
    }

    @VaneItem(
        name = "small_xp_bottle",
        base = Material.HONEY_BOTTLE,
        modelData = 0x76000b,
        version = 1,
    )
    /** Small XP bottle variant. */
    class SmallXpBottle(context: Context<Trifles?>) : XpBottle(context) {
        /** Default level capacity for small bottles. */
        fun configCapacityDef(): Int {
            return 10
        }
    }

    @VaneItem(
        name = "medium_xp_bottle",
        base = Material.HONEY_BOTTLE,
        modelData = 0x76000c,
        version = 1,
    )
    /** Medium XP bottle variant. */
    class MediumXpBottle(context: Context<Trifles?>) : XpBottle(context) {
        /** Default level capacity for medium bottles. */
        fun configCapacityDef(): Int {
            return 20
        }
    }

    @VaneItem(
        name = "large_xp_bottle",
        base = Material.HONEY_BOTTLE,
        modelData = 0x76000d,
        version = 1,
    )
    /** Large XP bottle variant. */
    class LargeXpBottle(context: Context<Trifles?>) : XpBottle(context) {
        /** Default level capacity for large bottles. */
        fun configCapacityDef(): Int {
            return 30
        }
    }

    @JvmField
    /** All XP bottle variants managed by this listener. */
    var bottles: MutableList<XpBottle> = ArrayList()

    /** Instantiates all XP bottle variants. */
    init {
        val context = requireNotNull(getContext())
        bottles.add(SmallXpBottle(context))
        bottles.add(MediumXpBottle(context))
        bottles.add(LargeXpBottle(context))
    }

    /** Exchanges consumed XP bottle items for experience and empty bottle items. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerItemConsume(event: PlayerItemConsumeEvent) {
        val module = module ?: return
        val item = event.item
        val customItem: org.oddlama.vane.core.item.api.CustomItem? =
            module.core?.itemRegistry()?.get(item)
        if (customItem !is XpBottle || !customItem.enabled()) {
            return
        }

        // Replace consumed custom bottle with empty bottle item.
        val player = event.player
        val mainHand = item == player.inventory.itemInMainHand
        PlayerUtil.removeOneItemFromHand(
            player,
            if (mainHand) EquipmentSlot.HAND else EquipmentSlot.OFF_HAND,
        )
        PlayerUtil.giveItem(player, CustomItemHelper.newStack("vane_trifles:empty_xp_bottle"))

        // Grant stored experience without triggering mending side effects.
        module.lastXpBottleConsumeTime[player.uniqueId] = System.currentTimeMillis()
        player.giveExp(expForLevel(customItem.configCapacity), false)
        player.world.playSound(
            player.location,
            Sound.ENTITY_PLAYER_LEVELUP,
            SoundCategory.PLAYERS,
            1.0f,
            1.0f,
        )

        // Cancel vanilla consumption because custom logic already handled item exchange.
        event.isCancelled = true
    }
}
