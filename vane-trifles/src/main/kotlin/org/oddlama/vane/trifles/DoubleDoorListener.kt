package org.oddlama.vane.trifles

import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockRedstoneEvent
import org.bukkit.event.entity.EntityInteractEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.SingleDoor.Companion.createDoorFromBlock

/** Keeps neighboring doors synchronized so double doors open and close together. */
class DoubleDoorListener(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group(
            "DoubleDoor",
            "Enable updating of double doors automatically when one of the doors is changed.",
        )
    ) {
    /** Synchronizes doors after a player right-click interaction. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hasBlock() && event.action == Action.RIGHT_CLICK_BLOCK) {
            if (event.player.isSneaking) {
                return
            }
            event.clickedBlock?.let(::handleDoubleDoor)
        }
    }

    /** Synchronizes doors after entity-triggered interactions (e.g. pressure plates). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityInteract(event: EntityInteractEvent) {
        val block = event.block
        handleDoubleDoor(block)
    }

    /** Synchronizes doors when redstone input toggles the block state. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockRedstone(event: BlockRedstoneEvent) {
        val now = event.newCurrent
        val old = event.oldCurrent
        if (now != old && (now == 0 || old == 0)) {
            // Only react to edge transitions.
            handleDoubleDoor(event.block)
        }
    }

    /** Finds the partner door and mirrors the primary door open state on the next tick. */
    fun handleDoubleDoor(block: Block) {
        val first = createDoorFromBlock(block) ?: return
        val second = first.secondDoor ?: return

        // Apply after current event processing so vanilla state changes have settled.
        scheduleNextTick {
            // Refresh both snapshots before copying state.
            if (!first.updateCachedState() || !second.updateCachedState()) {
                return@scheduleNextTick
            }
            second.isOpen = first.isOpen
        }
    }
}
