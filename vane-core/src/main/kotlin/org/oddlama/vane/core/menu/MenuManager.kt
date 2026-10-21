package org.oddlama.vane.core.menu

import java.util.*
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.config.TranslatedItemStack
import org.oddlama.vane.core.functional.Consumer2
import org.oddlama.vane.core.module.Context

/**
 * Tracks open menus, routes inventory events, and provides shared menu resources.
 *
 * @param context listener context.
 */
class MenuManager(context: Context<Core?>) : Listener<Core?>(context.namespace("Menus")) {
    /** Currently open menus keyed by player UUID. */
    private val openMenus = mutableMapOf<UUID, Menu>()

    /** Active menus keyed by their top inventory. */
    private val menus = mutableMapOf<Inventory, Menu>()

    /** Shared translated item used for accept actions in selectors. */
    @JvmField var itemSelectorAccept: TranslatedItemStack<*>?

    /** Shared translated item used for cancel actions in selectors. */
    @JvmField var itemSelectorCancel: TranslatedItemStack<*>?

    /** Shared translated item representing selected entries. */
    @JvmField var itemSelectorSelected: TranslatedItemStack<*>?

    /** Shared translated item used for generic selector page buttons. */
    @JvmField var genericSelectorPage: TranslatedItemStack<*>?

    /** Shared translated item indicating the current selector page. */
    @JvmField var genericSelectorCurrentPage: TranslatedItemStack<*>?

    /** Shared translated item used for generic selector filtering. */
    @JvmField var genericSelectorFilter: TranslatedItemStack<*>?

    /** Shared translated item used to cancel generic selection. */
    @JvmField var genericSelectorCancel: TranslatedItemStack<*>?

    /** Shared head selector configuration group. */
    @JvmField var headSelector: HeadSelectorGroup?

    init {
        val ctx = requireNotNull(getContext())
        headSelector = HeadSelectorGroup(ctx)

        val ctxItemSelector =
            ctx.namespace("ItemSelector", "Menu configuration for item selector menus.")
        itemSelectorAccept =
            TranslatedItemStack<Core?>(
                ctxItemSelector,
                "Accept",
                Material.LIME_TERRACOTTA,
                1,
                "Used to confirm item selection.",
            )
        itemSelectorCancel =
            TranslatedItemStack<Core?>(
                ctxItemSelector,
                "Cancel",
                Material.RED_TERRACOTTA,
                1,
                "Used to cancel item selection.",
            )
        itemSelectorSelected =
            TranslatedItemStack<Core?>(
                ctxItemSelector,
                "Selected",
                Material.BARRIER,
                1,
                "Represents the selected item. Left-clicking will reset the selection to the initial value, and right-clicking will clear the selected item. The given stack is used as the 'empty', cleared item.",
            )

        val ctxGenericSelector =
            ctx.namespace("GenericSelector", "Menu configuration for generic selector menus.")
        genericSelectorPage =
            TranslatedItemStack<Core?>(
                ctxGenericSelector,
                "Page",
                Material.PAPER,
                1,
                "Used to select pages.",
            )
        genericSelectorCurrentPage =
            TranslatedItemStack<Core?>(
                ctxGenericSelector,
                "CurrentPage",
                Material.MAP,
                1,
                "Used to indicate current page.",
            )
        genericSelectorFilter =
            TranslatedItemStack<Core?>(
                ctxGenericSelector,
                "Filter",
                Material.HOPPER,
                1,
                "Used to filter items.",
            )
        genericSelectorCancel =
            TranslatedItemStack<Core?>(
                ctxGenericSelector,
                "Cancel",
                Material.PRISMARINE_SHARD,
                1,
                "Used to cancel selection.",
            )
    }

    /** Resolves the open menu for a player from an inventory view. */
    fun menuFor(player: Player, view: InventoryView): Menu? = menuFor(player, view.topInventory)

    /** Resolves the open menu for a player from a top inventory reference. */
    fun menuFor(player: Player, inventory: Inventory?): Menu? {
        val menu = menus[inventory]
        val open = openMenus[player.uniqueId]
        if (open !== menu && menu != null) {
            module!!
                .log
                .warning(
                    "Menu inconsistency: entity $player accessed a menu '${openMenus[player.uniqueId]}' " +
                        "that isn't registered to it. The registered menu is '$menu'"
                )
            return menu
        }
        return menu ?: open
    }

    /** Registers a menu as open for a player. */
    fun add(player: Player, menu: Menu) {
        openMenus[player.uniqueId] = menu
        menus[menu.inventory()!!] = menu
    }

    /** Unregisters a menu for a player and removes global mapping when no viewer remains. */
    fun remove(player: Player, menu: Menu) {
        openMenus.remove(player.uniqueId)
        if (openMenus.values.none { it === menu }) menus.remove(menu.inventory())
    }

    /** Iterates over each currently open menu and its owning player. */
    fun forEachOpen(functor: Consumer2<Player?, Menu?>) {
        module!!.server.onlinePlayers.forEach { player ->
            val open = openMenus[player.uniqueId] ?: return@forEach
            functor.apply(player, open)
        }
    }

    /** Requests inventory refresh for all viewers of a menu. */
    fun update(menu: Menu?) {
        module!!
            .server
            .onlinePlayers
            .filter { openMenus[it.uniqueId] === menu }
            .forEach { it.updateInventory() }
    }

    /** Routes inventory clicks to the active menu and prevents vanilla handling. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val clicker = event.whoClicked as? Player ?: return
        val menu = menuFor(clicker, event.view) ?: return
        event.isCancelled = true
        val slot = if (event.clickedInventory === menu.inventory()) event.slot else -1
        menu.click(clicker, event.currentItem, slot, event)
    }

    /** Cancels inventory drag events for managed menus. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val clicker = event.whoClicked as? Player ?: return
        if (menuFor(clicker, event.view) != null) event.isCancelled = true
    }

    /** Notifies menus when their inventory is closed. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val human = event.player as? Player ?: return
        menuFor(human, event.view)?.closed(human, event.reason)
    }

    /** Prevents anvil repair-cost display for managed menu anvils. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPrepareAnvilEvent(event: PrepareAnvilEvent) {
        if (menus[event.view.topInventory] != null) event.view.repairCost = 0
    }
}
