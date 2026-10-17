package org.oddlama.vane.trifles.items

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Slime
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.PlayerUtil
import java.util.*

@VaneItem(name = "slime_bucket", base = Material.SLIME_BALL, modelData = 0x760014, version = 1)
/**
 * Captures tiny slimes into an item and updates model state based on slime chunk presence.
 */
class SlimeBucket(context: Context<Trifles?>) : org.oddlama.vane.core.item.CustomItem<Trifles?>(context) {
    /** Tracks players currently standing in slime chunks to detect transitions. */
    private val playersInSlimeChunks = HashSet<UUID>()

    /** Captures tiny slimes into slime bucket items when using an empty bucket. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        // Only capture tiny slimes.
        if (entity !is Slime || entity.size != 1) {
            return
        }

        if (entity.isDead) {
            return
        }

        // Require an empty bucket in hand.
        val player = event.player
        val itemInHand = player.equipment.getItem(event.hand)
        if (itemInHand.type != Material.BUCKET) {
            return
        }

        // Consume one empty bucket and create one slime bucket.
        entity.remove()
        PlayerUtil.swingArm(player, event.hand)
        player.playSound(player, Sound.ENTITY_SLIME_JUMP, SoundCategory.MASTER, 1.0f, 2.0f)

        // Set model data according to current chunk slime status.
        val newStack = newStack() ?: return
        newStack.editMeta { meta: ItemMeta ->
            val correctModelData: Float = if (player.chunk.isSlimeChunk)
                CUSTOM_MODEL_DATA_JUMPY
            else
                CUSTOM_MODEL_DATA_QUIET
            // The getter returns a copy, so the edited component has to be set back. The setter takes a
            // nullable component while the getter is non-null, so Kotlin offers no writable property here.
            @Suppress("UsePropertyAccessSyntax")
            meta.setCustomModelDataComponent(meta.customModelDataComponent.apply { floats = listOf(correctModelData) })

            val correctItemModel = if (player.chunk.isSlimeChunk)
                org.bukkit.NamespacedKey("vane_trifles", "slime_bucket_excited")
            else
                org.bukkit.NamespacedKey("vane_trifles", "slime_bucket")
            meta.itemModel = correctItemModel
        }

        if (itemInHand.amount == 1) {
            // Replace held bucket stack with the captured slime bucket.
            player.equipment.setItem(event.hand, newStack)
        } else {
            // Decrement stack and insert resulting slime bucket.
            itemInHand.amount -= 1
            PlayerUtil.giveItems(player, newStack, 1)
        }
    }

    /** Places captured slime back into the world and returns an empty bucket. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Only handle right-click block placement flow.
        if (!event.hasBlock() || event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        // Require slime bucket in main hand.
        val player = event.player
        val hand = event.hand ?: return
        val itemInHand = player.equipment.getItem(hand)
        val customItem: CustomItem? = module?.core?.itemRegistry()?.get(itemInHand)
        if (customItem !is SlimeBucket || !customItem.enabled()) {
            return
        }

        // Prevent secondary offhand actions from firing.
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        // Spawn back a tiny slime at interaction position.
        val loc = event.interactionPoint ?: return
        loc
            .getWorld()
            .spawnEntity(loc, EntityType.SLIME, CreatureSpawnEvent.SpawnReason.CUSTOM) { entity: Entity ->
                if (entity is Slime) {
                    entity.size = 1
                }
            }

        player.playSound(player, Sound.ENTITY_SLIME_JUMP, SoundCategory.MASTER, 1.0f, 2.0f)
        PlayerUtil.swingArm(player, hand)
        if (itemInHand.amount == 1) {
            // Replace held stack with an empty bucket.
            player.equipment.setItem(hand, ItemStack(Material.BUCKET))
        } else {
            // Decrement stack and add an empty bucket item.
            itemInHand.amount -= 1
            PlayerUtil.giveItems(player, ItemStack(Material.BUCKET), 1)
        }
    }

    /** Updates slime bucket model data whenever a player crosses slime chunk boundaries. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val module = module ?: return
        val inSlimeChunk = event.to.chunk.isSlimeChunk
        val inSet = playersInSlimeChunks.contains(player.uniqueId)

        if (inSet != inSlimeChunk) {
            if (inSlimeChunk) {
                playersInSlimeChunks.add(player.uniqueId)
            } else {
                playersInSlimeChunks.remove(player.uniqueId)
            }

            val correctModelData: Float = if (inSlimeChunk) CUSTOM_MODEL_DATA_JUMPY else CUSTOM_MODEL_DATA_QUIET
            val itemRegistry = module.core?.itemRegistry()
            for (item in player.inventory.contents) {
                val customItem: CustomItem? = itemRegistry?.get(item)
                if (customItem is SlimeBucket && customItem.enabled()) {
                    // Refresh custom model data on matching inventory items.
                    item?.editMeta { meta: ItemMeta ->
                        // The getter returns a copy, so the edited component has to be set back. The setter takes a
                        // nullable component while the getter is non-null, so Kotlin offers no writable property here.
                        @Suppress("UsePropertyAccessSyntax")
                        meta.setCustomModelDataComponent(meta.customModelDataComponent.apply { floats = listOf(correctModelData) })

                        val correctItemModel = if (inSlimeChunk)
                            org.bukkit.NamespacedKey("vane_trifles", "slime_bucket_excited")
                        else
                            org.bukkit.NamespacedKey("vane_trifles", "slime_bucket")
                        meta.itemModel = correctItemModel
                    }
                }
            }
        }
    }

    companion object {
        /** Custom model float for the non-jumpy slime bucket state. */
        private const val CUSTOM_MODEL_DATA_QUIET: Float = 0x760014.toFloat()

        /** Custom model float for the jumpy slime bucket state. */
        private const val CUSTOM_MODEL_DATA_JUMPY: Float = 0x760015.toFloat()
    }
}
