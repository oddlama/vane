package org.oddlama.vane.core.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

/** Contract for widgets rendered and interacted with inside a [Menu]. */
interface MenuWidget {
    /**
     * Updates this widget's visual state for the given menu.
     *
     * @return true when menu content changed.
     */
    fun update(menu: Menu?): Boolean

    /** Handles a click routed through the parent menu. */
    fun click(
        player: Player?,
        menu: Menu?,
        item: ItemStack?,
        slot: Int,
        event: InventoryClickEvent?,
    ): Menu.ClickResult?
}
