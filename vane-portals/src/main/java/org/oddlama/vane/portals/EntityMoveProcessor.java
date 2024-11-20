package org.oddlama.vane.portals;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.event.EntityMoveEvent;

public class EntityMoveProcessor extends ModuleComponent<Portals> {

    // This is the queue of entity move events that need processing.
    // It is a linked hash map, so we can update moved entity positions
    // without changing iteration order. Processed entries will be removed from
    // the front, and new entities are added to the back. If an entity moves twice
    // but wasn't processed, we don't need to update it. This ensures that no entities
    // will be accidentally skipped when we are struggling to keep up.
    // This stores entity_id -> (entity, old location).
    private LinkedHashMap<UUID, Pair<Entity, Location>> move_event_processing_queue = new LinkedHashMap<>();

    // Two hash maps to store old and current positions for each entity.
    private HashMap<UUID, Pair<Entity, Location>> move_event_current_positions = new HashMap<>();
    private HashMap<UUID, Pair<Entity, Location>> move_event_old_positions = new HashMap<>();

    private BukkitTask task;

    // Never process entity-move events for more than ~30% of a tick.
    // We use 15ms threshold time, and 50ms would be 1 tick.
    private static final long move_event_max_nanoseconds_per_tick = 15000000l;

    public EntityMoveProcessor(Context<Portals> context) {
        super(context);
    }

    private static boolean is_movement(final Location l1, final Location l2) {
        // Different worlds = not a movement event.
        return (
            l1.getWorld() == l2.getWorld() &&
            (l1.getX() != l2.getX() ||
                l1.getY() != l2.getY() ||
                l1.getZ() != l2.getZ() ||
                l1.getPitch() != l2.getPitch() ||
                l1.getYaw() != l2.getYaw())
        );
    }

    private void process_entity_movements() {
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

        final var active_portal_worlds = new HashSet<UUID>();
        for (final var portal : get_module().all_available_portals()) {
            if (get_module().is_activated(portal)) {
                active_portal_worlds.add(portal.spawn_world());
            }
        }

        // Store current positions for each entity
        for (final var world_id : active_portal_worlds) {
            final var world = get_module().getServer().getWorld(world_id);
            if (world != null) {
                for (final var entity : world.getEntities()) {
                    move_event_current_positions.put(entity.getUniqueId(), Pair.of(entity, entity.getLocation()));
                }
            }
        }

        // For each entity that has an old position (computed efficiently via Sets.intersection),
        // but isn't yet contained in the entities to process, we check whether the position
        // has changed. If so, we add the entity to the processing queue.
        // If the processing queue already contained the entity, we remove it before iterating
        // as there is nothing to do - we simply lose information about the intermediate position.
        for (final var eid : Sets.difference(
            Sets.intersection(move_event_old_positions.keySet(), move_event_current_positions.keySet()),
            move_event_processing_queue.keySet()
        )) {
            final var old_entity_and_loc = move_event_old_positions.get(eid);
            final var new_entity_and_loc = move_event_current_positions.get(eid);
            if (
                old_entity_and_loc == null ||
                new_entity_and_loc == null ||
                !is_movement(old_entity_and_loc.getRight(), new_entity_and_loc.getRight())
            ) {
                continue;
            }

            move_event_processing_queue.put(eid, Pair.of(old_entity_and_loc));
        }

        // Swap old and current position hash maps, and only retain the now-old positions.
        // This avoids unnecessary allocations.
        final var tmp = move_event_current_positions;
        move_event_current_positions = move_event_old_positions;
        move_event_old_positions = tmp;
        move_event_current_positions.clear();

        // Phase 2 - Event dispatching
        // --------------------------------------------

        final var time_begin = System.nanoTime();
        final var pm = get_module().getServer().getPluginManager();
        final var iter = move_event_processing_queue.entrySet().iterator();
        while (iter.hasNext()) {
            final var e_and_old_loc = iter.next().getValue();
            iter.remove();

            // Dispatch event.
            final var entity = e_and_old_loc.getLeft();
            final var event = new EntityMoveEvent(entity, e_and_old_loc.getRight(), entity.getLocation());
            pm.callEvent(event);

            // Abort if we exceed the threshold time
            final var time_now = System.nanoTime();
            if (time_now - time_begin > move_event_max_nanoseconds_per_tick) {
                break;
            }
        }
    }

    @Override
    protected void on_enable() {
        // Each tick we need to recalculate whether entities moved.
        // This is using a scheduling algorithm (see function implementation) to
        // keep it lightweight and to prevent lags.
        task = schedule_task_timer(this::process_entity_movements, 1l, 1l);
    }

    @Override
    protected void on_disable() {
        task.cancel();
    }
}
