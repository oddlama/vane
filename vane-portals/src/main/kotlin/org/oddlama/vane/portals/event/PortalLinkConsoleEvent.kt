package org.oddlama.vane.portals.event

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.oddlama.vane.portals.portal.Portal

/**
 * Fired while validating or linking a console to a portal.
 *
 * @property player the acting player.
 * @property console the console block to link.
 * @property portalBlocks blocks that will become part of the portal.
 * @property checkOnly whether this is a dry-run validation.
 * @property portal existing portal when relinking, or null when creating.
 */
class PortalLinkConsoleEvent(
    @JvmField val player: Player,
    @JvmField val console: Block?,
    val portalBlocks: MutableList<Block?>?,
    private val checkOnly: Boolean,
    @JvmField val portal: Portal?
) : PortalEvent() {
    // Java accessors from before v1.22.0. The public fields stay for plugins built against later releases.
    fun getPlayer(): Player = player
    fun getConsole(): Block? = console
    fun getPortal(): Portal? = portal

    /** Controls whether non-owners are automatically denied. */
    private var cancelIfNotOwner = true

    /** Sets whether linking is automatically cancelled for non-owners. */
    fun setCancelIfNotOwner(cancelIfNotOwner: Boolean) {
        this.cancelIfNotOwner = cancelIfNotOwner
    }

    /** Returns whether this event is in validation-only mode. */
    fun checkOnly() = checkOnly

    /** Returns Bukkit handlers for this event. */
    override fun getHandlers() = handlerList

    /** Returns true when explicitly cancelled or blocked by ownership checks. */
    override fun isCancelled() =
        super.isCancelled() || (cancelIfNotOwner && portal != null && player.uniqueId != portal.owner())

    /** Static Bukkit handler list for this event type. */
    companion object {
        /** Shared handler list used by Bukkit's event system. */
        @JvmStatic
        val handlerList: HandlerList = HandlerList()
    }
}
