package org.oddlama.vane.core.menu

import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.functional.Consumer2
import org.oddlama.vane.core.module.Context
import java.util.logging.Level

/**
 * Base menu abstraction with widget support and click lifecycle handling.
 */
open class Menu {
    /**
     * Menu manager that tracks this menu instance.
     */
    protected val manager: MenuManager

    /**
     * Backing inventory shown to viewers.
     */
    protected var inventory: Inventory? = null

    /**
     * Registered menu widgets.
     */
    private val widgets: MutableSet<MenuWidget> = mutableSetOf()

    /**
     * Callback executed when the menu is closed for non-player reasons.
     */
    var onClose: Consumer2<Player?, InventoryCloseEvent.Reason?>? = null
        private set

    /**
     * Callback executed when the player naturally closes the menu.
     */
    var onNaturalClose: Consumer1<Player?>? = null
        private set

    /**
     * Optional arbitrary menu tag.
     */
    private var tag: Any? = null

    /**
     * Whether the menu is tainted and should not be opened.
     */
    protected var tainted: Boolean = false

    /**
     * Creates a menu without assigning an inventory yet.
     */
    protected constructor(context: Context<*>) {
        manager = context.module!!.core!!.menuManager!!
    }

    /**
     * Creates a menu bound to a pre-created inventory.
     */
    constructor(context: Context<*>, inventory: Inventory?) {
        manager = context.module!!.core!!.menuManager!!
        this.inventory = inventory
    }

    /** Returns the menu manager. */
    fun manager(): MenuManager = manager

    /** Returns the backing inventory. */
    fun inventory(): Inventory? = inventory

    /** Returns the arbitrary menu tag. */
    fun tag(): Any? = tag

    /** Sets an arbitrary menu tag. */
    fun tag(tag: Any?): Menu = apply { this.tag = tag }

    /** Marks this menu as tainted. */
    fun taint() {
        tainted = true
    }

    /** Adds a widget to this menu. */
    fun add(widget: MenuWidget?) {
        widgets.add(widget!!)
    }

    /** Removes a widget from this menu. */
    fun remove(widget: MenuWidget?): Boolean = widgets.remove(widget)

    /** Updates this menu without forcing a client refresh. */
    fun update() = update(false)

    /**
     * Updates widgets and refreshes viewers when needed.
     */
    open fun update(forceUpdate: Boolean) {
        val updated = widgets.count { it.update(this) }
        if (updated > 0 || forceUpdate) manager.update(this)
    }

    /**
     * Opens the backing inventory window for a player.
     */
    open fun openWindow(player: Player) {
        if (!tainted) player.openInventory(inventory!!)
    }

    /**
     * Opens this menu for a player.
     */
    fun open(player: Player) {
        if (tainted) return
        update(true)
        manager.scheduleNextTick {
            manager.add(player, this)
            openWindow(player)
        }
    }

    /**
     * Closes this menu for a player.
     */
    @JvmOverloads
    fun close(player: Player, reason: InventoryCloseEvent.Reason = InventoryCloseEvent.Reason.PLUGIN): Boolean {
        if (player.openInventory.topInventory !== inventory) {
            try {
                throw RuntimeException("Invalid close from unrelated menu.")
            } catch (e: RuntimeException) {
                manager.module!!.log.log(
                    Level.WARNING,
                    "Tried to close menu inventory that isn't opened by the player $player", e
                )
            }
            return false
        }
        manager.scheduleNextTick { player.closeInventory(reason) }
        return true
    }

    /** Registers the non-natural close callback. */
    fun onClose(onClose: Consumer2<Player?, InventoryCloseEvent.Reason?>?): Menu = apply { this.onClose = onClose }

    /** Registers the natural-close callback. */
    fun onNaturalClose(onNaturalClose: Consumer1<Player?>?): Menu = apply { this.onNaturalClose = onNaturalClose }

    /**
     * Called by the manager when this menu is closed.
     */
    fun closed(player: Player, reason: InventoryCloseEvent.Reason?) {
        dispatchClose(player, reason, onNaturalClose, onClose)
        inventory!!.clear()
        manager.remove(player, this)
    }

    /**
     * Hook for menu-level click handling.
     */
    open fun onClick(player: Player?, item: ItemStack?, slot: Int, event: InventoryClickEvent?): ClickResult =
        ClickResult.IGNORE

    /**
     * Dispatches a click to menu and widget handlers.
     */
    fun click(player: Player, item: ItemStack?, slot: Int, event: InventoryClickEvent) {
        if (event.action == InventoryAction.UNKNOWN) return

        var result = ClickResult.IGNORE
        result = ClickResult.or(result, onClick(player, item, slot, event))
        for (widget in widgets) {
            result = ClickResult.or(result, widget.click(player, this, item, slot, event) ?: ClickResult.IGNORE)
        }

        when (result) {
            ClickResult.INVALID_CLICK, ClickResult.IGNORE -> {}
            ClickResult.SUCCESS -> player.playSound(
                player.location,
                Sound.UI_BUTTON_CLICK,
                SoundCategory.MASTER,
                1f,
                1f
            )

            ClickResult.ERROR -> player.playSound(
                player.location,
                Sound.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.MASTER,
                1f,
                1f
            )
        }
    }

    /**
     * Result values for menu click handling.
     *
     * @param priority precedence used when combining results.
     */
    enum class ClickResult(private val priority: Int) {
        IGNORE(0), INVALID_CLICK(1), SUCCESS(2), ERROR(3);

        /**
         * Helpers for combining click results.
         */
        companion object {
            /** Returns the result with higher priority. */
            fun or(a: ClickResult, b: ClickResult): ClickResult = if (a.priority > b.priority) a else b
        }
    }

    /**
     * Static click utility helpers.
     */
    companion object {
        @JvmStatic
                /** Returns whether the event is a left or right click. */
        fun isLeftOrRightClick(event: InventoryClickEvent?): Boolean {
            val type = event?.click ?: return false
            return type == ClickType.LEFT || type == ClickType.RIGHT
        }

        @JvmStatic
                /** Returns whether the event is a left click. */
        fun isLeftClick(event: InventoryClickEvent?): Boolean =
            event?.click == ClickType.LEFT

        /**
         * Runs the close callback for [reason]. A player closing the menu runs [onNaturalClose]
         * if there is one; every other close, and a player close without it, runs [onClose].
         */
        internal fun dispatchClose(
            player: Player?,
            reason: InventoryCloseEvent.Reason?,
            onNaturalClose: Consumer1<Player?>?,
            onClose: Consumer2<Player?, InventoryCloseEvent.Reason?>?,
        ) {
            if (reason == InventoryCloseEvent.Reason.PLAYER && onNaturalClose != null) {
                onNaturalClose.apply(player)
            } else {
                onClose?.apply(player, reason)
            }
        }
    }
}
