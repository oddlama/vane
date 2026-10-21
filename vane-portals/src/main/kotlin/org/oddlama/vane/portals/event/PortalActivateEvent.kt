package org.oddlama.vane.portals.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal

/**
 * Fired when a portal is activated.
 *
 * @property player the player that triggered the activation, if any.
 * @property portal the source portal.
 * @property target the connected target portal.
 */
class PortalActivateEvent(
    @JvmField val player: Player?,
    @JvmField val portal: Portal?,
    val target: Portal?,
) : PortalEvent() {
    // Java accessors from before v1.22.0. The public fields stay for plugins built against later
    // releases.
    fun getPlayer(): Player? = player

    fun getPortal(): Portal? = portal

    /** Returns Bukkit handlers for this event. */
    override fun getHandlers() = handlerList

    /** Static Bukkit handler list for this event type. */
    companion object {
        /** Shared handler list used by Bukkit's event system. */
        @JvmStatic val handlerList: HandlerList = HandlerList()
    }
}
