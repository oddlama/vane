package org.oddlama.vane.portals;

import com.destroystokyo.paper.event.entity.EntityTeleportEndGatewayEvent;
import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.util.Vector;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.portals.event.EntityMoveEvent;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.util.Nms;

public class PortalTeleporter extends Listener<Portals> {

	private final HashMap<UUID, Location> entities_portalling = new HashMap<>();

	public PortalTeleporter(Context<Portals> context) {
		super(context);
	}

	private boolean cancel_portal_event(final Entity entity) {
		if (entities_portalling.containsKey(entity.getUniqueId())) {
			return true;
		}

		return get_module().is_portal_block(entity.getLocation().getBlock());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_portal(final PlayerPortalEvent event) {
		if (cancel_portal_event(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_entity_teleport_event(final EntityTeleportEvent event) {
		if (cancel_portal_event(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_entity_teleport_end_gateway_event(final EntityTeleportEndGatewayEvent event) {
		// End gateway teleport can be initiated when the bounding boxes overlap, so
		// the entity location will not necessarily be at the position where the end gateway block is.
		// Therefore, we additionally check whether the initiating end gateway block is part of a portal structure.
		// Otherwise, this event would already be handled by EntityTeleportEvent.
		if (get_module().is_portal_block(event.getGateway().getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_teleport_event(final PlayerTeleportEndGatewayEvent event) {
		final var block = event.getGateway().getBlock();
		if (get_module().is_portal_block(block)) {
			event.setCancelled(true);
		}
	}

	private void teleport_single_entity(final Entity entity, final Location target_location, final Vector new_velocity) {
		final var entity_location = entity.getLocation();
		final var nms_entity = Nms.entity_handle(entity);

		// Now teleport. There are many ways to do it, but some are preferred over others.
		// Primarily, entity.teleport() will dismount any passengers. Meh.
		if (target_location.getWorld() == entity_location.getWorld()) {
			if (entity instanceof Player player) {
				// For players traveling in the same world, we can use the NMS player's connection's
				// teleport method, which only modifies player position without dismounting.
				Nms.get_player(player).connection.teleport(target_location.getX(), target_location.getY(), target_location.getZ(), target_location.getYaw(), target_location.getPitch());
			} else {
				// Similarly, we can just move entities.
				nms_entity.absMoveTo(target_location.getX(), target_location.getY(), target_location.getZ(), target_location.getYaw(), target_location.getPitch());
			}

			// For some unknown reason (SPIGOT-619) we always need to set the yaw again.
			nms_entity.setYHeadRot(target_location.getYaw());
		} else {
			final var passengers = new ArrayList<Entity>(entity.getPassengers());

			// Entities traveling to a different dimension need to be despawned and respawned as both worlds are distinct levels.
			// This means they must be dismounted (or unridden) before teleportation.
			passengers.stream().forEach(entity::removePassenger);
			entity.teleport(target_location);

			for (var p : passengers) {
				teleport_single_entity(p, target_location, new Vector());
				entity.addPassenger(p);
			}
		}

		// Retain velocity. Previously we needed to force-set it in the next tick,
		// as apparently the movement event overrides might override the velocity.
		// Now we are using our own movement events which are run outside any
		// entity ticking, so no such workaround is necessary.
		//schedule_next_tick(() -> {
		//entity.setVelocity(new_velocity);
		//});
		entity.setVelocity(new_velocity);
	}

	private void teleport_entity(final Entity entity, final Portal source, Portal target) {
		var target_location = target.spawn().clone();
		if (entity instanceof LivingEntity living_entity) {
			// Entities in vehicles are teleported when the vehicle is teleported.
			if (living_entity.isInsideVehicle()) {
				return;
			}


			// Increase Y value if an entity is currently flying through a portal that
			// has extent in the y direction (i.e., is built upright)
			if (living_entity.isGliding() && source.orientation().plane().y()) {
				target_location.setY(target_location.getY() + 1.5);
			}
		}

		// Put null to signal initiated teleportation
		final var entity_id = entity.getUniqueId();
		entities_portalling.put(entity_id, null);

		// First copy pitch & yaw to target, will be transformed soon.
		final var entity_location = entity.getLocation();
		target_location.setPitch(entity_location.getPitch());
		target_location.setYaw(entity_location.getYaw());

		// If the exit orientation of the target portal is locked, we make sure that
		// the orientation of the entered portal is flipped if an entity (player) enters from the back.
		// We have to flip the source portal orientation if the vector to be transformed
		// is NOT opposing the source portal vector (i.e., not pointing against the front).
		// Calculate new location (pitch, yaw) and velocity.
		final var source_orientation = source.orientation();
		target_location = source_orientation.apply(target.orientation(), target_location, target.exit_orientation_locked());
		final var new_velocity = source_orientation.apply(target.orientation(), entity.getVelocity(), target.exit_orientation_locked());

		teleport_single_entity(entity, target_location, new_velocity);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_entity_move(final EntityMoveEvent event) {
		final var entity = event.getEntity();
		final var entity_id = entity.getUniqueId();
		final var block = event.getTo().getBlock();

		if (!entities_portalling.containsKey(entity_id)) {
			// Check if we walked into a portal
			if (!get_module().portal_area_materials.contains(block.getType())) {
				return;
			}

			final var portal = get_module().portal_for(block);
			if (portal == null) {
				return;
			}

			final var target = get_module().connected_portal(portal);
			if (target == null) {
				return;
			}

			teleport_entity(entity, portal, target);
		} else {
			final var loc = entities_portalling.get(entity_id);
			if (loc == null) {
				// Initial teleport. Remember the current location, so we can check
				// that the entity moved away far enough to allow another teleport
				entities_portalling.put(entity_id, event.getFrom().clone());
			} else if (!get_module().portal_area_materials.contains(block.getType())) {
				// At least 2 blocks away and outside of portal area → finish portalling.
				if (loc.getWorld() == event.getFrom().getWorld() && event.getFrom().distance(loc) > 2.0) {
					entities_portalling.remove(entity_id);
				}
			}
		}
	}
}
