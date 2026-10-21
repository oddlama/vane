package org.oddlama.vane.trifles

import java.util.*
import net.kyori.adventure.text.Component
import org.bukkit.Nameable
import org.bukkit.NamespacedKey
import org.bukkit.block.Container
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.items.storage.Backpack
import org.oddlama.vane.trifles.items.storage.Pouch
import org.oddlama.vane.util.StorageUtil

/** Coordinates storage-item inventory opening, safety checks, and persistence. */
class StorageGroup(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group("Storage", "Extensions to storage related stuff will be grouped under here.")
    ) {
    /** Open transient inventories mapped to owner UUID and backing item stack reference. */
    private val openBlockStateInventories: MutableMap<Inventory, Pair<UUID, ItemStack>> =
        Collections.synchronizedMap(HashMap())

    /** Action-bar message shown when trying to open stacked storage items. */
    @LangMessage var langOpenStackedItem: TranslatedMessage? = null

    /** Supports right-click swap insertion of non-storage items into storage item inventories. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlaceItemInStorageInventory(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) {
            return
        }

        // Ignore managed transient inventories to avoid desynchronization and item loss.
        val ownerAndItem = openBlockStateInventories[event.inventory]
        if (ownerAndItem != null) {
            return
        }

        // Allow right-click cursor insertion into single, non-stacked storage items.
        val currentItem = event.currentItem
        if (
            event.click == ClickType.RIGHT &&
                event.action == InventoryAction.SWAP_WITH_CURSOR &&
                isStorageItem(currentItem) &&
                currentItem?.amount == 1
        ) {
            // Never nest storage items within other storage items.
            if (!isStorageItem(event.cursor)) {
                val customItem: CustomItem? = module?.core?.itemRegistry()?.get(currentItem)

                // Only custom storage items support this swap-in behavior.
                if (customItem != null) {
                    currentItem.editMeta(BlockStateMeta::class.java) { meta ->
                        val blockState = meta.blockState
                        if (blockState is Container) {
                            val leftovers = blockState.inventory.addItem(event.cursor)
                            event.view.setCursor(leftovers.values.firstOrNull())
                            meta.blockState = blockState
                        }
                    }
                }
            }

            // Always cancel this click path because we handled cursor state manually.
            event.isCancelled = true
            return
        }
    }

    /** Restricts inventory interactions while a storage container inventory is open. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        getOwnerAndItemIfValid(event.inventory, player) ?: return

        val clickedItemIsStorage = isStorageItem(event.currentItem)
        val cursorItemIsStorage = isStorageItem(event.cursor)
        val clickedInventoryIsPlayerInventory = event.clickedInventory === player.inventory

        var cancel: Boolean
        when (event.action) {
            InventoryAction.DROP_ALL_CURSOR,
            InventoryAction.DROP_ALL_SLOT,
            InventoryAction.DROP_ONE_CURSOR,
            InventoryAction.DROP_ONE_SLOT ->
                // Allow dropping storage item because we handle this elsewhere
                cancel = false

            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_ONE,
            InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR ->
                // Only deny placing storage items in storage inventory
                cancel = cursorItemIsStorage && !clickedInventoryIsPlayerInventory

            InventoryAction.MOVE_TO_OTHER_INVENTORY ->
                // Only deny moving storage item from player inventory to storage inventory
                cancel = clickedItemIsStorage && clickedInventoryIsPlayerInventory

            InventoryAction.PICKUP_ALL,
            InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE,
            InventoryAction.PICKUP_SOME ->
                // Always allow pickup
                cancel = false

            else ->
                // Restrictive default prevents moving of any storage items
                cancel = clickedItemIsStorage || cursorItemIsStorage
        }

        when (event.click) {
            ClickType.NUMBER_KEY -> {
                // Deny swapping storage items with number keys into storage inventory, but allow in
                // player inventory
                val swappedItemIsStorage =
                    isStorageItem(player.inventory.getItem(event.hotbarButton))
                cancel =
                    (swappedItemIsStorage || clickedItemIsStorage) &&
                        !clickedInventoryIsPlayerInventory
            }

            else -> {
                /* no-op */
            }
        }

        if (cancel) {
            event.isCancelled = true
        }
    }

    /**
     * Prevents dropping the currently open storage item without closing its transient inventory.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onDropItem(event: PlayerDropItemEvent) {
        if (!isStorageItem(event.itemDrop.itemStack)) {
            return
        }

        // Close managed inventory when its backing item gets dropped.
        val storageItemIsOpenState = isCurrentlyOpen(event.itemDrop.itemStack)
        if (storageItemIsOpenState) {
            val isKnownCustomInventory =
                openBlockStateInventories.containsKey(event.player.openInventory.topInventory)
            if (isKnownCustomInventory) {
                event.player.closeInventory(InventoryCloseEvent.Reason.CANT_USE)
            } else {
                // Repair stale open tag on legacy/bugged items.
                event.itemDrop.itemStack.editMeta { meta: ItemMeta ->
                    meta.persistentDataContainer.set(
                        STORAGE_IS_OPEN,
                        PersistentDataType.BOOLEAN,
                        false,
                    )
                }
            }
        }
    }

    /** Ensures picked-up storage items are always marked as closed. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPickupItem(event: EntityPickupItemEvent) {
        if (event.entity !is Player) {
            return
        }

        if (!isStorageItem(event.item.itemStack)) {
            return
        }

        // Normalize stale metadata on pickup.
        event.item.itemStack.editMeta { meta: ItemMeta ->
            meta.persistentDataContainer.set(
                STORAGE_IS_OPEN,
                PersistentDataType.BOOLEAN,
                false,
            )
        }
    }

    /** Prevents dragging storage items into an opened storage inventory. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        getOwnerAndItemIfValid(event.inventory, player) ?: return

        // Disallow storage-in-storage nesting through drag operations.
        for (itemStack in event.newItems.values) {
            if (isStorageItem(itemStack)) {
                event.isCancelled = true
                return
            }
        }
    }

    /** Persists managed inventory changes after click interactions. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun saveAfterClick(event: InventoryClickEvent) {
        saveInventoryChanges(event.whoClicked, event.inventory)
    }

    /** Persists managed inventory changes after drag interactions. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun saveAfterDrag(event: InventoryDragEvent) {
        saveInventoryChanges(event.whoClicked, event.inventory)
    }

    /** Writes current transient inventory content back into the backing storage item. */
    private fun saveInventoryChanges(
        whoClicked: org.bukkit.entity.HumanEntity,
        inventory: Inventory,
    ) {
        if (whoClicked !is Player) {
            return
        }

        val ownerAndItem = openBlockStateInventories[inventory]
        if (ownerAndItem == null || ownerAndItem.first != whoClicked.uniqueId) {
            return
        }

        updateStorageItem(ownerAndItem.second, inventory, whoClicked)
    }

    /** Final persistence and cleanup when a managed transient inventory is closed. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun saveAfterClose(event: InventoryCloseEvent) {
        val ownerAndItem = openBlockStateInventories[event.inventory]
        if (ownerAndItem == null || ownerAndItem.first != event.player.uniqueId) {
            return
        }

        // Mark backing item as closed before writing inventory content.
        ownerAndItem.second.editMeta { meta: ItemMeta ->
            meta.persistentDataContainer.set(
                STORAGE_IS_OPEN,
                PersistentDataType.BOOLEAN,
                false,
            )
        }
        updateStorageItem(ownerAndItem.second, event.inventory, event.player as Player)
        openBlockStateInventories.remove(event.inventory)
    }

    /** Returns owner/item mapping only when the player owns the tracked inventory session. */
    private fun getOwnerAndItemIfValid(
        inventory: Inventory,
        player: Player,
    ): Pair<UUID, ItemStack>? {
        val ownerAndItem = openBlockStateInventories[inventory] ?: return null
        return if (ownerAndItem.first == player.uniqueId) ownerAndItem else null
    }

    /** Returns whether an item is treated as a storage item by this module. */
    private fun isStorageItem(item: ItemStack?): Boolean {
        if (item == null) {
            return false
        }

        val customItem: CustomItem? = module?.core?.itemRegistry()?.get(item)
        if (customItem != null && (customItem is Backpack || customItem is Pouch)) {
            return true
        }

        // Treat shulker-like block-state containers as storage items as well.
        val itemMeta = item.itemMeta
        return itemMeta is BlockStateMeta && itemMeta.blockState is ShulkerBox
    }

    /** Returns whether a storage item is currently flagged as open. */
    private fun isCurrentlyOpen(item: ItemStack?): Boolean {
        if (item == null || !item.hasItemMeta()) {
            return false
        }
        return item.persistentDataContainer.getOrDefault(
            STORAGE_IS_OPEN,
            PersistentDataType.BOOLEAN,
            false,
        )
    }

    /**
     * Updates the backing storage item with transient inventory contents and handles moved items.
     */
    private fun updateStorageItem(item: ItemStack, inventory: Inventory, player: Player) {
        // Recover backing item reference if the tracked stack moved during interaction.
        var currentItem = item
        if (currentItem.type.isAir && inventory.holder is Player) {
            // Check cursor item first.
            val cursorItem: ItemStack = player.openInventory.cursor
            if (cursorItem.hasItemMeta() && isCurrentlyOpen(cursorItem)) {
                currentItem = cursorItem
                openBlockStateInventories[inventory] = Pair(player.uniqueId, currentItem)
            } else {
                // Fallback: scan inventory slots for the active open marker.
                for (checkedItem in player.inventory.contents) {
                    if (checkedItem == null || !checkedItem.hasItemMeta()) {
                        continue
                    }
                    if (isCurrentlyOpen(checkedItem)) {
                        currentItem = checkedItem
                        openBlockStateInventories[inventory] = Pair(player.uniqueId, currentItem)
                        break
                    }
                }
            }
        }
        currentItem.editMeta(BlockStateMeta::class.java) { meta ->
            val blockState = meta.blockState
            if (blockState is Container) {
                blockState.inventory.contents = inventory.contents
                meta.blockState = blockState
            }
        }
    }

    /**
     * Opens a transient inventory view for the item-backed container.
     *
     * @return `true` when opening succeeded.
     */
    fun openBlockStateInventory(player: Player, item: ItemStack): Boolean {
        // Require container-backed block-state metadata.
        val itemMeta = item.itemMeta
        if (itemMeta !is BlockStateMeta || itemMeta.blockState !is Container) {
            return false
        }

        // Managed storage inventories only support single-item stacks.
        if (item.amount != 1) {
            langOpenStackedItem?.sendActionBar(player)
            return false
        }

        val blockStateMeta: BlockStateMeta = itemMeta
        val container = blockStateMeta.blockState as Container

        // Reuse item naming for the opened transient inventory title.
        var name: Component?
        name =
            if (blockStateMeta.hasDisplayName()) blockStateMeta.displayName()
            else if (blockStateMeta.hasItemName()) blockStateMeta.itemName() else null

        // Fallback to registered custom item display name for unnamed legacy items.
        if (name == null) {
            val customItem: CustomItem? = module?.core?.itemRegistry()?.get(item)
            name = if (customItem != null) customItem.displayName() else Component.text("")
        }
        (container as Nameable).customName(name)

        // Create transient inventory and copy persisted container contents.
        val server = module?.server ?: return false
        val transientInventory: Inventory =
            server.createInventory(player, container.inventory.type, name ?: Component.text(""))
        transientInventory.contents = container.inventory.contents

        // Track open session and show inventory to player.
        openBlockStateInventories[transientInventory] = Pair(player.uniqueId, item)
        player.openInventory(transientInventory)
        return true
    }

    companion object {
        /** Boolean metadata key indicating whether a storage item is currently open. */
        val STORAGE_IS_OPEN: NamespacedKey =
            StorageUtil.namespacedKey("vane_trifles", "currently_opened_storage")
    }
}
