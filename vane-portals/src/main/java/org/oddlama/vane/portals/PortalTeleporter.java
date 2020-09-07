package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import java.util.HashMap;
import java.util.Iterator;
import org.bukkit.event.block.Action;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.oddlama.vane.core.module.Context;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.Listener;

public class PortalTeleporter extends Listener<Portals> {
	private final HashMap<UUID, Location> entities_portalling = new HashMap<>();

	public PortalTeleporter(Context<Portals> context) {
		super(context);
	}

	private boolean cancel_portal_event(final Entity entity) {
		if (entities_portalling.containsKey(entity.getUniqueId())) {
			return true;
		}

		if (get_module().is_portal_block(entity.getLocation().getBlock())) {
			return true;
		}

		return false;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_portal(final PlayerPortalEvent event) {
		if (cancel_portal_event(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_entity_portal_event(final EntityPortalEvent event) {
		if (cancel_portal_event(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_move(final PlayerMoveEvent event) {
		final var player = event.getPlayer();
		final var player_id = player.getUniqueId();

		if (!entities_portalling.containsKey(player_id)) {
			// Check if we walked into a portal
			final var block = event.getTo().getBlock();
			if (block.getType() != Portals.MATERIAL_PORTAL) {
				return;
			}

			final var portal = get_module().portal_for(block);
			if (portal == null) {
				return;
			}

			final var target = portal.target();
			if (target == null) {
				return;
			}

			// Put null to signal initiated teleportation
			entities_portalling.put(player_id, null);

			var target_location = target.spawn().clone();
			final var player_location = player.getLocation();
			target_location.setPitch(player_location.getPitch());
			target_location.setYaw(player_location.getYaw());

			// Calculate new pitch, yaw and velocity
			target_location = portal.orientation().apply(target.orientation(), target_location);
			final var new_velocity = portal.orientation().apply(target.orientation(), player.getVelocity());

			// Set new movement location
			event.setTo(target_location);
			get_module().log.info("portal player " + player + " from " + portal + " to " + target);

			// Retain velocity
			player.setVelocity(new_velocity);
		} else {
			// We just portalled
			// TODO ???? if (event.getFrom().getBlock().getType() != PortalsConfiguration.PORTAL_DEFAULT_MATERIAL_PORTAL_AREA_ACTIVATED) {
			// TODO ???? 	return;
			// TODO ???? }

			final var loc = entities_portalling.get(player_id);
			if (loc == null) {
				// Initial teleport. Remember current location, so we can check
				// that the entity moved away far enough to allow another teleport
				entities_portalling.put(player_id, event.getFrom().clone());
			} else {
				// At least 2 blocks away → finish portalling.
				if (event.getFrom().distance(loc) > 2.0) {
					entities_portalling.remove(player_id);
				}
			}
		}
	}
}
