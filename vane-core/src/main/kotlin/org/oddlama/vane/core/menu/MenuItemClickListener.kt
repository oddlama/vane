package org.oddlama.vane.core.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.core.functional.Function3
import org.oddlama.vane.core.functional.Function4

/**
 * Lightweight widget that only listens for clicks on a specific slot.
 *
 * @param slot target slot.
 * @param onClick click callback.
 */
class MenuItemClickListener(
    /** Slot to listen for clicks on. */
    private val slot: Int,
    /** Callback invoked when the configured slot is clicked. */
    private val onClick:
        Function4<Player?, Menu?, ItemStack?, InventoryClickEvent?, Menu.ClickResult?>?,
) : MenuWidget {
    /** Convenience constructor with a left-click-only callback. */
    constructor(
        slot: Int,
        onClick: Function3<Player?, Menu?, ItemStack?, Menu.ClickResult?>,
    ) : this(
        slot,
        Function4 { player, menu, item, event ->
            if (!Menu.isLeftClick(event)) Menu.ClickResult.INVALID_CLICK
            else onClick.apply(player, menu, item)
        },
    )

    /** Returns the listened slot. */
    fun slot(): Int = slot

    /** Click-only listener has no update behavior. */
    override fun update(menu: Menu?): Boolean = false

    /** Handles click events for the configured slot. */
    override fun click(
        player: Player?,
        menu: Menu?,
        item: ItemStack?,
        slot: Int,
        event: InventoryClickEvent?,
    ): Menu.ClickResult {
        if (this.slot != slot) return Menu.ClickResult.IGNORE
        return onClick?.apply(player, menu, item, event) ?: Menu.ClickResult.IGNORE
    }
}
