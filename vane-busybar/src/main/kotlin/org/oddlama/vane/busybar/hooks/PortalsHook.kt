package org.oddlama.vane.busybar.hooks

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.json.JSONObject
import org.oddlama.vane.busybar.BusyBar
import org.oddlama.vane.portals.event.PortalActivateEvent
import org.oddlama.vane.portals.event.PortalDestroyEvent

/**
 * Forwards vane-portals events to the bridges of the players involved.
 *
 * @param busybar owning module.
 */
class PortalsHook(private val busybar: BusyBar) : Listener {
    /** Tells a player's bridge which portal they just activated. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPortalActivate(event: PortalActivateEvent) {
        val player = event.player ?: return
        busybar.stream.emit(
            "portal.activate",
            JSONObject()
                .put("player", player.name)
                .put("from", event.portal?.name() ?: UNNAMED)
                .put("to", event.target?.name() ?: UNNAMED),
            player.uniqueId,
            busybar.access.receivePortals,
        )
    }

    /** Tells the owner's bridge when one of their portals is destroyed. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPortalDestroy(event: PortalDestroyEvent) {
        if (event.checkOnly()) return
        val owner = event.portal.owner() ?: return
        busybar.stream.emit(
            "portal.destroy",
            JSONObject().put("portal", event.portal.name() ?: UNNAMED).put("by", event.player.name),
            owner,
            busybar.access.receivePortals,
        )
    }

    companion object {
        /** Name used for portals without one. */
        private const val UNNAMED = "Portal"
    }
}
