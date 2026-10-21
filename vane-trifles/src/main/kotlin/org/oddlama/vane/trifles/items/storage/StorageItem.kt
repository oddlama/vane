package org.oddlama.vane.trifles.items.storage

import org.bukkit.Sound
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.StorageGroup
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.PlayerUtil

/**
 * Base item for portable storage containers backed by block-state inventories.
 *
 * @param context module context used to register the custom item.
 */
abstract class StorageItem(context: Context<Trifles?>) :
    org.oddlama.vane.core.item.CustomItem<Trifles?>(context) {

    /** Initializes item metadata so new storage items start in a closed state. */
    override fun updateItemStack(itemStack: ItemStack): ItemStack {
        // Tag every created storage item as closed by default.
        itemStack.editMeta { meta: ItemMeta ->
            meta.persistentDataContainer.set(
                StorageGroup.STORAGE_IS_OPEN,
                PersistentDataType.BOOLEAN,
                false,
            )
        }
        return itemStack
    }

    /** Opens the storage inventory when the matching storage item is right-clicked. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onPlayerRightClick(event: PlayerInteractEvent) {
        if (!event.hasItem() || event.useItemInHand() == Event.Result.DENY) {
            return
        }

        // Open on either right-click air or right-click block.
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) {
            return
        }

        // Ensure the interacted item resolves to this exact storage item class.
        val player = event.player
        val hand = event.hand ?: return
        val item = player.equipment.getItem(hand)
        val customItem = module?.core?.itemRegistry()?.get(item)
        if (customItem?.javaClass != this::class.java || !customItem.enabled()) {
            return
        }

        // Reset all storage item open flags in player inventory.
        for (invItem in player.inventory.contents) {
            if (invItem != null && invItem.hasItemMeta()) {
                invItem.editMeta { meta: ItemMeta ->
                    if (meta.persistentDataContainer.has(StorageGroup.STORAGE_IS_OPEN)) {
                        meta.persistentDataContainer.set(
                            StorageGroup.STORAGE_IS_OPEN,
                            PersistentDataType.BOOLEAN,
                            false,
                        )
                    }
                }
            }
        }

        // Mark the currently opened storage item.
        item.editMeta { meta: ItemMeta ->
            meta.persistentDataContainer.set(
                StorageGroup.STORAGE_IS_OPEN,
                PersistentDataType.BOOLEAN,
                true,
            )
        }

        // Prevent follow-up vanilla interaction processing.
        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        if (module?.storageGroup?.openBlockStateInventory(player, item) == true) {
            player.world.playSound(player, Sound.ITEM_BUNDLE_DROP_CONTENTS, 1.0f, 1.2f)
            PlayerUtil.swingArm(player, hand)
        }
    }
}
