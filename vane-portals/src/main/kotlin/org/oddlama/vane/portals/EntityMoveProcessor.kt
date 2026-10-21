package org.oddlama.vane.portals

import java.util.*
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.plugin.PluginManager
import org.bukkit.scheduler.BukkitTask
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.portals.event.EntityMoveEvent

/**
 * Tracks entity movement near active portals and emits [EntityMoveEvent] events.
 *
 * This exists because the default server move event coverage is incomplete for all entity types.
 */
class EntityMoveProcessor(context: Context<Portals?>?) : ModuleComponent<Portals?>(context) {
    /**
     * Queue of movement events waiting for dispatch.
     *
     * This stores `entityId -> (entity, oldLocation)` and preserves FIFO order.
     */
    private val moveEventProcessingQueue = LinkedHashMap<UUID?, Pair<Entity, Location?>>()

    /** Snapshot map for entity positions of the current tick. */
    private var moveEventCurrentPositions = HashMap<UUID?, Pair<Entity?, Location?>?>()

    /** Snapshot map for entity positions of the previous tick. */
    private var moveEventOldPositions = HashMap<UUID?, Pair<Entity?, Location?>?>()

    /** Periodic task processing movement detection and dispatch. */
    private var task: BukkitTask? = null

    /** Detects movements and dispatches queued [EntityMoveEvent] events with a time budget. */
    private fun processEntityMovements() {
        val module = module ?: return

        // This custom event detector is necessary as PaperMC's entity move events trigger for
        // LivingEntities,
        // but we need move events for all entities. Wanna throw that potion through the portal?
        // Yes. Shoot players through a portal? Ohh, definitely. Throw junk right into their bases?
        // Abso-fucking-lutely.

        // This implementation uses a priority queue and a small
        // scheduling algorithm to prevent this function from ever causing lags.
        // Lags caused by other plugins or external means will inherently cause
        // the entity movement event tickrate to be slowed down.
        //
        // This function is called every tick and has two main phases.
        //
        // 1. Detect entity movement and queue entities for processing.
        // 2. Iterate through entities that moved in FIFO order
        //    and call event handlers, but make sure to immediately abort
        //    processing after exceeding a threshold time. This ensures
        //    that it will always at least process one entity, but never
        //    hog any performance from other tasks.

        // Phase 1 - Movement detection
        // --------------------------------------------

        val activePortalWorlds =
            module
                .allAvailablePortals()
                .filterNotNull()
                .filter(module::isActivated)
                .mapNotNull { portal -> portal.spawnWorld() }
                .toHashSet()

        // Store current positions for each entity
        for (worldId in activePortalWorlds) {
            val world: World? = module.server.getWorld(worldId)
            if (world != null) {
                for (entity in world.entities) {
                    moveEventCurrentPositions[entity.uniqueId] = Pair(entity, entity.location)
                }
            }
        }

        // For each entity that has an old position (computed efficiently via Sets.intersection),
        // but isn't yet contained in the entities to process, we check whether the position
        // has changed. If so, we add the entity to the processing queue.
        // If the processing queue already contained the entity, we remove it before iterating
        // as there is nothing to do - we simply lose information about the intermediate position.
        for (eid in
            (moveEventOldPositions.keys intersect moveEventCurrentPositions.keys) -
                moveEventProcessingQueue.keys) {
            val oldEntityAndLoc = moveEventOldPositions[eid]
            val newEntityAndLoc = moveEventCurrentPositions[eid]
            if (
                oldEntityAndLoc == null ||
                    newEntityAndLoc == null ||
                    !isMovement(
                        oldEntityAndLoc.second!!,
                        newEntityAndLoc.second!!,
                    )
            ) {
                continue
            }

            // oldEntityAndLoc contains nullable entity/location; the processing queue requires a
            // non-null entity
            moveEventProcessingQueue[eid] = Pair(oldEntityAndLoc.first!!, oldEntityAndLoc.second)
        }

        // Swap old and current position hash maps, and only retain the now-old positions.
        // This avoids unnecessary allocations.
        val tmp = moveEventCurrentPositions
        moveEventCurrentPositions = moveEventOldPositions
        moveEventOldPositions = tmp
        moveEventCurrentPositions.clear()

        // Phase 2 - Event dispatching
        // --------------------------------------------
        val timeBegin = System.nanoTime()
        val pm: PluginManager = module.server.pluginManager
        val iter: MutableIterator<MutableMap.MutableEntry<UUID?, Pair<Entity, Location?>>?> =
            moveEventProcessingQueue.entries.iterator()
        while (iter.hasNext()) {
            val eAndOldLoc = iter.next()!!.value
            iter.remove()

            // Dispatch event.
            val entity = eAndOldLoc.first
            val event = EntityMoveEvent(entity, eAndOldLoc.second, entity.location)
            pm.callEvent(event)

            // Abort if we exceed the threshold time
            val timeNow = System.nanoTime()
            if (timeNow - timeBegin > MOVE_EVENT_MAX_NANOSECONDS_PER_TICK) {
                break
            }
        }
    }

    /** Starts periodic movement processing. */
    override fun onEnable() {
        // Each tick we need to recalculate whether entities moved.
        // This is using a scheduling algorithm (see function implementation) to
        // keep it lightweight and to prevent lags.
        task = scheduleTaskTimer({ processEntityMovements() }, 1L, 1L)
    }

    /** Stops periodic movement processing. */
    override fun onDisable() {
        task?.cancel()
    }

    /** Constants and helpers for movement processing. */
    companion object {
        /**
         * Hard upper bound for movement event processing time per tick.
         *
         * 15ms is roughly 30% of a 50ms server tick.
         */
        private const val MOVE_EVENT_MAX_NANOSECONDS_PER_TICK = 15000000L

        /** Returns true when two locations represent movement within the same world. */
        private fun isMovement(l1: Location, l2: Location): Boolean {
            // Different worlds = not a movement event.
            return (l1.world === l2.world &&
                (l1.x != l2.x ||
                    l1.y != l2.y ||
                    l1.z != l2.z ||
                    l1.pitch != l2.pitch ||
                    l1.yaw != l2.yaw))
        }
    }
}
