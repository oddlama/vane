package org.oddlama.vane.util

import java.util.concurrent.ThreadLocalRandom
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Vector
import org.oddlama.vane.util.BlockUtil.dropNaturally

/** Player-oriented inventory and interaction utility helpers. */
object PlayerUtil {
    /** Spawns particles used for elytra boost effects. */
    @JvmStatic
    fun spawnElytraBoostParticles(player: Player) {
        val loc = player.location
        val vel = player.velocity.length()
        repeat(16) {
            val rnd = Vector.getRandom().subtract(Vector(.5, .5, .5)).normalize().multiply(.25)
            val dir = rnd.clone().multiply(.5).subtract(player.velocity)
            loc.world.spawnParticle(
                Particle.FIREWORK,
                loc.add(rnd),
                0,
                dir.x,
                dir.y,
                dir.z,
                vel * ThreadLocalRandom.current().nextDouble(0.4, 0.6),
            )
        }
    }

    /** Applies an elytra directional velocity boost to a player. */
    @JvmStatic
    fun applyElytraBoost(player: Player, factor: Double) {
        val v = player.location.direction.normalize().multiply(factor)
        player.velocity = player.velocity.add(v)
        player.world.playSound(
            player.location,
            Sound.ENTITY_ENDER_DRAGON_FLAP,
            SoundCategory.PLAYERS,
            0.4f,
            2.0f,
        )
    }

    /** Removes exactly one item from the specified hand slot. */
    @JvmStatic
    fun removeOneItemFromHand(player: Player, hand: EquipmentSlot) {
        val item = player.equipment.getItem(hand)
        if (item.amount == 1) {
            player.inventory.setItem(hand, null)
        } else {
            item.amount -= 1
            player.inventory.setItem(hand, item)
        }
    }

    /** Returns whether a player owns at least the required quantities for each item key. */
    @JvmStatic
    fun hasItems(player: Player, items: Map<ItemStack?, Int>): Boolean {
        if (player.gameMode == GameMode.CREATIVE) return true
        val inventory = player.inventory
        return items.all { (item, amount) ->
            inventory.containsAtLeast(item!!.clone().also { it.amount = 1 }, amount)
        }
    }

    /** Removes the amount specified by an item's own amount field. */
    @JvmStatic
    fun takeItems(player: Player, item: ItemStack): Boolean =
        takeItems(player, mutableMapOf(item to item.amount))

    /** Removes multiple item requirements from a player's inventory. */
    @JvmStatic
    fun takeItems(player: Player, items: Map<ItemStack?, Int>): Boolean {
        if (player.gameMode == GameMode.CREATIVE) return true
        if (!hasItems(player, items)) return false

        val stacks =
            items.entries.flatMap { (item, amount) -> createLawfulStacks(item!!, amount).toList() }
        val leftovers = player.inventory.removeItem(*stacks.toTypedArray())
        if (leftovers.isNotEmpty()) {
            Bukkit.getLogger()
                .warning(
                    "[vane] Unexpected leftovers while removing the following items from a player's inventory: $stacks"
                )
            leftovers.forEach { (_, leftover) ->
                Bukkit.getLogger()
                    .warning("[vane] Leftover: ${leftover.type}, amount: ${leftover.amount}")
            }
            return false
        }
        return true
    }

    /** Gives a single item stack to a player. */
    @JvmStatic
    fun giveItem(player: Player, item: ItemStack?) {
        item?.let { giveItems(player, arrayOf(it)) }
    }

    /** Splits an amount into legal stack sizes for the given item type. */
    fun createLawfulStacks(item: ItemStack, amount: Int): Array<ItemStack> {
        val maxStack = item.maxStackSize
        val stacks = (maxStack - 1 + amount) / maxStack
        if (stacks < 1) return emptyArray()

        val leftover = amount % maxStack
        return Array(stacks) { i ->
            item.clone().also { clone ->
                clone.amount = if (i == stacks - 1 && leftover != 0) leftover else maxStack
            }
        }
    }

    /** Gives a certain amount of an item by creating lawful stacks. */
    @JvmStatic
    fun giveItems(player: Player, item: ItemStack, amount: Int) {
        giveItems(player, createLawfulStacks(item, amount))
    }

    /** Gives stacks to a player and drops leftovers at their location. */
    fun giveItems(player: Player, items: Array<ItemStack>) {
        val leftovers = player.inventory.addItem(*items)
        leftovers.values.forEach { item ->
            player.location.world.dropItem(player.location, item).pickupDelay = 0
        }
    }

    /** Attempts to till a block into farmland while honoring break events. */
    @JvmStatic
    fun tillBlock(player: Player, block: Block): Boolean {
        val breakEvent = BlockBreakEvent(block, player)
        Bukkit.getPluginManager().callEvent(breakEvent)
        if (breakEvent.isCancelled) return false

        block.type = Material.FARMLAND
        player.world.playSound(
            player.location,
            Sound.ITEM_HOE_TILL,
            SoundCategory.BLOCKS,
            1.0f,
            1.0f,
        )
        return true
    }

    /** Attempts to plant a crop block and consume one seed item when needed. */
    @JvmStatic
    fun seedBlock(
        player: Player,
        usedItem: ItemStack,
        block: Block,
        plantType: Material,
        seedType: Material,
    ): Boolean {
        val below = block.getRelative(BlockFace.DOWN)
        val placeEvent =
            BlockPlaceEvent(block, below.state, below, usedItem, player, true, EquipmentSlot.HAND)
        Bukkit.getPluginManager().callEvent(placeEvent)
        if (placeEvent.isCancelled) return false

        if (player.gameMode != GameMode.CREATIVE) {
            val seedstack = ItemStack(seedType, 1)
            if (!player.inventory.containsAtLeast(seedstack, 1)) return false
            player.inventory.removeItem(seedstack)
        }

        block.type = plantType
        (block.blockData as Ageable).also { ageable ->
            ageable.age = 0
            block.blockData = ageable
        }

        val sound =
            if (seedType == Material.NETHER_WART) Sound.ITEM_NETHER_WART_PLANT
            else Sound.ITEM_CROP_PLANT
        player.world.playSound(player.location, sound, SoundCategory.BLOCKS, 1.0f, 1.0f)
        return true
    }

    /** Harvests a mature plant, resets growth age, and drops produce. */
    @JvmStatic
    fun harvestPlant(player: Player, block: Block): Boolean {
        val drops =
            when (block.type) {
                Material.WHEAT ->
                    arrayOf(ItemStack(Material.WHEAT, 1 + (Math.random() * 2.5).toInt()))
                Material.CARROTS ->
                    arrayOf(ItemStack(Material.CARROT, 1 + (Math.random() * 2.5).toInt()))
                Material.POTATOES ->
                    arrayOf(ItemStack(Material.POTATO, 1 + (Math.random() * 2.5).toInt()))
                Material.BEETROOTS ->
                    arrayOf(ItemStack(Material.BEETROOT, 1 + (Math.random() * 2.5).toInt()))
                Material.NETHER_WART ->
                    arrayOf(ItemStack(Material.NETHER_WART, 1 + (Math.random() * 2.5).toInt()))
                else -> return false
            }

        val ageable = block.blockData as? Ageable ?: return false
        if (ageable.age != ageable.maximumAge) return false

        val breakEvent = BlockBreakEvent(block, player)
        Bukkit.getPluginManager().callEvent(breakEvent)
        if (breakEvent.isCancelled) return false

        ageable.age = 0
        block.blockData = ageable
        drops.forEach { dropNaturally(block, it) }
        return true
    }

    /** Plays the arm swing animation for a chosen hand slot. */
    @JvmStatic
    fun swingArm(player: Player, hand: EquipmentSlot) {
        when (hand) {
            EquipmentSlot.HAND -> player.swingMainHand()
            EquipmentSlot.OFF_HAND -> player.swingOffHand()
            else -> {}
        }
    }
}
