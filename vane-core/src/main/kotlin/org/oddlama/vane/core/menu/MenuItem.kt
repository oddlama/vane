package org.oddlama.vane.core.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.functional.Function4

/**
 * Slot-bound widget that renders an item and handles click callbacks.
 *
 * @param slot target slot in the menu inventory.
 * @param item initial item value, or null for auto-updating items.
 * @param onClick click callback for this menu item.
 */
open class MenuItem
@JvmOverloads
constructor(
    /** Inventory slot managed by this menu item widget. */
    private val slot: Int,
    item: ItemStack?,
    /** Click callback invoked when this slot is clicked. */
    private val onClick:
        Function4<Player?, Menu?, MenuItem?, InventoryClickEvent?, Menu.ClickResult?>? =
        null,
) : MenuWidget {
    /** Current rendered item value. */
    private var item: ItemStack? = null

    /** Whether the widget should refresh its item before each update. */
    private val autoUpdate: Boolean = item == null

    /** Convenience constructor with a left-click-only callback. */
    constructor(
        slot: Int,
        item: ItemStack?,
        onClick: Function3<Player?, Menu?, MenuItem?, Menu.ClickResult?>,
    ) : this(
        slot,
        item,
        Function4 { player, menu, self, event ->
            if (!Menu.isLeftClick(event)) Menu.ClickResult.INVALID_CLICK
            else onClick.apply(player, menu, self)
        },
    )

    init {
        item(item)
    }

    /** Returns the target slot. */
    fun slot(): Int = slot

    /** Returns the item currently in this slot for the given menu. */
    fun item(menu: Menu?): ItemStack? = menu?.inventory()?.getItem(slot)

    /** Updates the internal item value. */
    open fun item(item: ItemStack?) {
        this.item = item
    }

    /** Updates the internal item and refreshes the menu. */
    fun updateItem(menu: Menu, item: ItemStack?) {
        this.item(item)
        menu.update()
    }

    /** Renders this widget into the menu inventory when needed. */
    override fun update(menu: Menu?): Boolean {
        if (menu == null) return false
        if (autoUpdate) item(null as ItemStack?)
        val cur = item(menu)
        return if (cur !== item) {
            menu.inventory()!!.setItem(slot(), item)
            true
        } else false
    }

    /** Handles click events for this slot. */
    override fun click(
        player: Player?,
        menu: Menu?,
        item: ItemStack?,
        slot: Int,
        event: InventoryClickEvent?,
    ): Menu.ClickResult? {
        if (this.slot != slot) return Menu.ClickResult.IGNORE
        return onClick?.apply(player, menu, this, event) ?: Menu.ClickResult.IGNORE
    }
}
