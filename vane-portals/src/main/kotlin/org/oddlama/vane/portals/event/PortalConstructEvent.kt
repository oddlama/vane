package org.oddlama.vane.portals.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.PortalBoundary

/**
 * Fired while validating or constructing a new portal from a boundary.
 *
 * @property player the player initiating construction.
 * @property boundary the detected boundary.
 * @property checkOnly whether this is a dry-run validation.
 */
class PortalConstructEvent(
    @JvmField val player: Player?,
    @JvmField val boundary: PortalBoundary?,
    private val checkOnly: Boolean
) :
    PortalEvent() {
    // Java accessors from before v1.22.0. The public fields stay for plugins built against later releases.
    fun getPlayer(): Player? = player
    fun getBoundary(): PortalBoundary? = boundary

    /** Returns whether this event is in validation-only mode. */
    fun checkOnly() = checkOnly

    /** Returns Bukkit handlers for this event. */
    override fun getHandlers() = handlerList

    /** Static Bukkit handler list for this event type. */
    companion object {
        /** Shared handler list used by Bukkit's event system. */
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}
