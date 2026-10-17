package org.oddlama.vane.portals.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal

/**
 * Fired while validating or executing portal destruction.
 *
 * @property player the acting player.
 * @property portal the portal targeted for destruction.
 * @property checkOnly whether this is a dry-run validation.
 */
class PortalDestroyEvent(@JvmField val player: Player, @JvmField val portal: Portal, private val checkOnly: Boolean) :
    PortalEvent() {
    // Java accessors from before v1.22.0. The public fields stay for plugins built against later releases.
    fun getPlayer(): Player = player
    fun getPortal(): Portal = portal

    /** Controls whether non-owners are automatically denied. */
    private var cancelIfNotOwner = true

    /** Sets whether destruction is automatically cancelled for non-owners. */
    fun setCancelIfNotOwner(cancelIfNotOwner: Boolean) {
        this.cancelIfNotOwner = cancelIfNotOwner
    }

    /** Returns whether this event is in validation-only mode. */
    fun checkOnly() = checkOnly

    /** Returns Bukkit handlers for this event. */
    override fun getHandlers() = handlerList

    /** Returns true when explicitly cancelled or blocked by ownership checks. */
    override fun isCancelled() = super.isCancelled() || (cancelIfNotOwner && player.uniqueId != portal.owner())

    /** Static Bukkit handler list for this event type. */
    companion object {
        /** Shared handler list used by Bukkit's event system. */
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}
