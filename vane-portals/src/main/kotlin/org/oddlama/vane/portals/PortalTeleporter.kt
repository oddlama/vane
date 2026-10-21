package org.oddlama.vane.portals

import com.destroystokyo.paper.event.entity.EntityTeleportEndGatewayEvent
import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent
import java.util.*
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.util.Vector
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.portals.event.EntityMoveEvent
import org.oddlama.vane.portals.portal.Portal
import org.oddlama.vane.util.Nms

/**
 * Handles teleportation of entities and players through connected portals.
 *
 * Manages entity transfer, velocity/orientation transformation and passenger handling.
 */
class PortalTeleporter(context: Context<Portals?>?) : Listener<Portals?>(context) {
    /** Tracks entities currently in the custom teleport flow. */
    private val entitiesPortalling = HashMap<UUID, Location?>()

    /** Returns true when native portal events should be cancelled for [entity]. */
    private fun cancelPortalEvent(entity: Entity) =
        entity.uniqueId in entitiesPortalling || module!!.isPortalBlock(entity.location.block)

    /** Cancels vanilla player portal processing while custom portal logic is active. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerPortal(event: PlayerPortalEvent) {
        if (cancelPortalEvent(event.player)) {
            event.isCancelled = true
        }
    }

    /** Cancels vanilla entity teleport events while custom portal logic is active. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityTeleportEvent(event: EntityTeleportEvent) {
        if (cancelPortalEvent(event.entity)) {
            event.isCancelled = true
        }
    }

    /** Cancels end-gateway teleports initiated from portal structure blocks. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onEntityTeleportEndGatewayEvent(event: EntityTeleportEndGatewayEvent) {
        // End gateway teleport can be initiated when the bounding boxes overlap, so
        // the entity location will not necessarily be at the position where the end gateway block
        // is.
        // Therefore, we additionally check whether the initiating end gateway block is part of a
        // portal structure.
        // Otherwise, this event would already be handled by EntityTeleportEvent.
        if (module!!.isPortalBlock(event.gateway.block)) {
            event.isCancelled = true
        }
    }

    /** Cancels player end-gateway teleports initiated from portal structure blocks. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onPlayerTeleportEvent(event: PlayerTeleportEndGatewayEvent) {
        val block = event.gateway.block
        if (module!!.isPortalBlock(block)) {
            event.isCancelled = true
        }
    }

    /**
     * Teleports a single [entity] to [targetLocation] and reapplies [newVelocity], preserving
     * mount/passenger relationships when possible.
     */
    private fun teleportSingleEntity(
        entity: Entity,
        targetLocation: Location,
        newVelocity: Vector,
    ) {
        val entityLocation = entity.location
        val nmsEntity = Nms.entityHandle(entity)

        // Now teleport. There are many ways to do it, but some are preferred over others.
        // Primarily, entity.teleport() will dismount any passengers. Meh.
        if (targetLocation.world === entityLocation.world) {
            if (entity is Player) {
                // For players traveling in the same world, we can use the NMS player's connection's
                // teleport method, which only modifies player position without dismounting.
                Nms.getPlayer(entity)
                    .connection
                    .teleport(
                        targetLocation.x,
                        targetLocation.y,
                        targetLocation.z,
                        targetLocation.yaw,
                        targetLocation.pitch,
                    )
            } else {
                // Similarly, we can just move entities.
                nmsEntity.absSnapTo(
                    targetLocation.x,
                    targetLocation.y,
                    targetLocation.z,
                    targetLocation.yaw,
                    targetLocation.pitch,
                )
            }

            // For some unknown reason (SPIGOT-619) we always need to set the yaw again.
            nmsEntity.yHeadRot = targetLocation.yaw
        } else {
            val passengers = ArrayList(entity.passengers)

            // Entities traveling to a different dimension need to be despawned and respawned as
            // both worlds are distinct levels.
            // This means they must be dismounted (or unridden) before teleportation.
            passengers.forEach { passenger -> entity.removePassenger(passenger) }
            entity.teleport(targetLocation)

            for (p in passengers) {
                teleportSingleEntity(p, targetLocation, Vector())
                entity.addPassenger(p)
            }
        }

        // Retain velocity. Previously we needed to force-set it in the next tick,
        // as apparently the movement event overrides might override the velocity.
        // Now we are using our own movement events which are run outside any
        // entity ticking, so no such workaround is necessary.
        // scheduleNextTick(() -> {
        // entity.setVelocity(newVelocity);
        // });
        entity.velocity = newVelocity
    }

    /**
     * Computes transformed orientation/velocity and teleports [entity] from [source] to [target].
     */
    private fun teleportEntity(entity: Entity, source: Portal, target: Portal) {
        var targetLocation = target.spawn().clone()
        if (entity is LivingEntity) {
            // Entities in vehicles are teleported when the vehicle is teleported.
            if (entity.isInsideVehicle) {
                return
            }

            // Increase Y value if an entity is currently flying through a portal that
            // has extent in the y direction (i.e., is built upright)
            if (entity.isGliding && source.orientation()?.plane()?.y() == true) {
                targetLocation.y += 1.5
            }
        }

        // Put null to signal initiated teleportation
        val entityId = entity.uniqueId
        entitiesPortalling[entityId] = null

        // First copy pitch & yaw to target, will be transformed soon.
        val entityLocation = entity.location
        targetLocation.pitch = entityLocation.pitch
        targetLocation.yaw = entityLocation.yaw

        // If the exit orientation of the target portal is locked, we make sure that
        // the orientation of the entered portal is flipped if an entity (player) enters from the
        // back.
        // We have to flip the source portal orientation if the vector to be transformed
        // is NOT opposing the source portal vector (i.e., not pointing against the front).
        // Calculate new location (pitch, yaw) and velocity.
        val sourceOrientation = source.orientation() ?: return
        val targetOrientation = target.orientation() ?: return
        targetLocation =
            sourceOrientation.apply(
                targetOrientation,
                targetLocation,
                target.exitOrientationLocked(),
            )
        val newVelocity =
            sourceOrientation.apply(
                targetOrientation,
                entity.velocity,
                target.exitOrientationLocked(),
            )

        teleportSingleEntity(entity, targetLocation, newVelocity)
    }

    /** Detects portal entry/exit from movement events and runs portal teleport flow. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityMove(event: EntityMoveEvent) {
        val module = module ?: return
        val entity = event.entity ?: return
        val entityId = entity.uniqueId
        val from = event.from ?: return
        val to = event.to ?: return
        val block = to.block

        if (entityId !in entitiesPortalling) {
            // Check if we walked into a portal
            if (!module.portalAreaMaterials.contains(block.type)) {
                return
            }

            val portal: Portal = module.portalFor(block) ?: return

            val target: Portal = module.connectedPortal(portal) ?: return

            teleportEntity(entity, portal, target)
        } else {
            val loc = entitiesPortalling[entityId]
            if (loc == null) {
                // Initial teleport. Remember the current location, so we can check
                // that the entity moved away far enough to allow another teleport
                entitiesPortalling[entityId] = from.clone()
            } else if (!module.portalAreaMaterials.contains(block.type)) {
                // At least 2 blocks away and outside of portal area → finish portalling.
                if (loc.world === from.world && from.distance(loc) > 2.0) {
                    entitiesPortalling.remove(entityId)
                }
            }
        }
    }
}
