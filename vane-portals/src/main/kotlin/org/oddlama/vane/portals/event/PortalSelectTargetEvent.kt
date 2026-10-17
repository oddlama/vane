package org.oddlama.vane.portals.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal

/**
 * Fired before a portal target is changed or selected.
 *
 * @property player the acting player.
 * @property portal the source portal.
 * @property target the requested target portal.
 * @property checkOnly whether this is a dry-run validation.
 */
class PortalSelectTargetEvent(
    @JvmField val player: Player?,
    @JvmField val portal: Portal?,
    @JvmField val target: Portal?,
    private val checkOnly: Boolean
) : PortalEvent() {
    // Java accessors from before v1.22.0. The public fields stay for plugins built against later releases.
    fun getPlayer(): Player? = player
    fun getPortal(): Portal? = portal
    fun getTarget(): Portal? = target

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
