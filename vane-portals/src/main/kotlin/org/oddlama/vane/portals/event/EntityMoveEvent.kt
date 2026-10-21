package org.oddlama.vane.portals.event

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Synthetic event fired by the move processor for tracked entity movement.
 *
 * @property entity the moved entity.
 * @property from previous location.
 * @property to new location.
 */
class EntityMoveEvent(
    @JvmField val entity: Entity?,
    @JvmField val from: Location?,
    @JvmField val to: Location?,
) : Event() {
    // Java accessors from before v1.22.0. The public fields stay for plugins built against later
    // releases.
    fun getEntity(): Entity? = entity

    fun getFrom(): Location? = from

    fun getTo(): Location? = to

    /** Returns Bukkit handlers for this event. */
    override fun getHandlers() = handlerList

    /** Static Bukkit handler list for this event type. */
    companion object {
        /** Shared handler list used by Bukkit's event system. */
        @JvmStatic val handlerList: HandlerList = HandlerList()
    }
}
