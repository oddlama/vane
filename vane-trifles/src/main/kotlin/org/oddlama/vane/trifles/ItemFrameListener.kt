package org.oddlama.vane.trifles

import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

/** Toggles item frame invisibility when players use shears on a frame. */
class ItemFrameListener(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group(
            "InvisibleItemFrame",
            "Right clicking on an item frame with shears equipped will make it disappear.",
        )
    ) {
    /** Handles shears interaction on item frames and toggles invisibility. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEvent(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked
        if (
            player.inventory.itemInMainHand.type != Material.SHEARS ||
                entity.type != EntityType.ITEM_FRAME
        ) {
            return
        }

        event.isCancelled = true
        entity.isInvisible = !entity.isInvisible
    }
}
