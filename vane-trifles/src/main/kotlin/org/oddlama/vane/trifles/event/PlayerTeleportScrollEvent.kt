package org.oddlama.vane.trifles.event

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * Fired when a scroll teleport is about to move a player.
 */
class PlayerTeleportScrollEvent(
    player: Player,
    /** Source location of the teleport. */
    val from: Location,
    /** Destination location of the teleport. */
    val to: Location?
) :
    PlayerEvent(player), Cancellable {

    /** Backing cancellation flag. */
    private var cancelled = false

    /** Returns whether this teleport should be cancelled. */
    override fun isCancelled(): Boolean = cancelled

    /** Updates cancellation state for this event. */
    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    /** Returns Bukkit handlers for this event type. */
    override fun getHandlers(): HandlerList = HANDLER_LIST

    companion object {
        /** Shared Bukkit handler list for this event type. */
        @JvmField
        val HANDLER_LIST: HandlerList = HandlerList()

        /** Static accessor required by Bukkit's event registration API. */
        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLER_LIST
    }
}
