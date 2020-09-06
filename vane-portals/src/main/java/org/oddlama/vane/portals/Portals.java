package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.Chunk;
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

@VaneModule(name = "portals", bstats = 8642, config_version = 1, lang_version = 1, storage_version = 1)
public class Portals extends Module<Portals> {
	public static final Material MATERIAL_BOUNDARY = Material.OBSIDIAN;
	// TODO custom origin block?
	public static final Material MATERIAL_ORIGIN = Material.NETHERITE_BLOCK;
	public static final Material MATERIAL_CONSOLE = Material.ENCHANTING_TABLE;

	public Portals() {
		new PortalActivator(this);
		new PortalBlockProtector(this);
		new PortalConstructor(this);
	}

	public Portal portal_for(final Block block) {
		// TODO
		return new Portal();
	}

	public boolean is_portal_block(final Block block) {
		return portal_for(block) != null;
	}

	public Portal controlled_portal(final Block block) {
		final var root_portal = portal_for(block);
		if (root_portal != null) {
			return root_portal;
		}

		for (final var adj : adjacent_blocks_3d(block)) {
			if (adj.getType() == MATERIAL_CONSOLE) {
				final var portal = portal_for(block);
				if (portal != null) {
					return portal;
				}
			}
		}

		return null;
	}

	private void disable_consoles_in_chunk(final Chunk chunk) {
		// TODO
	}

	private void enable_consoles_in_chunk(final Chunk chunk) {
		// TODO
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_monitor_chunk_unload(final ChunkUnloadEvent event) {
		disable_consoles_in_chunk(event.getChunk());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_monitor_chunk_load(final ChunkLoadEvent event) {
		enable_consoles_in_chunk(event.getChunk());
	}

	//@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	//public void on_player_portal(final PlayerPortalEvent event) {
	//	if (entitiesPortalling.containsKey(event.getPlayer().getUniqueId())) {
	//		event.setCancelled(true);
	//		return;
	//	}

	//	if (PortalBlock.query(event.getPlayer().getLocation().getBlock()) != null)
	//		event.setCancelled(true);
	//}

	//@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	//public void on_entity_portal_event(final EntityPortalEvent event) {
	//	if (entitiesPortalling.containsKey(event.getEntity().getUniqueId())) {
	//		event.setCancelled(true);
	//		return;
	//	}

	//	if (PortalBlock.query(event.getEntity().getLocation().getBlock()) != null)
	//		event.setCancelled(true);
	//}

	//@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	//public void on_player_move(final PlayerMoveEvent event) {
	//	if (!entitiesPortalling.containsKey(event.getPlayer().getUniqueId())) {
	//		/* check if we walked into a portal */
	//		Block block = event.getTo().getBlock();
	//		if (block.getType() != PortalsConfiguration.PORTAL_DEFAULT_MATERIAL_PORTAL_AREA_ACTIVATED)
	//			return;

	//		Portal portal = Portal.query(block);
	//		if (portal == null)
	//			return;

	//		Portal target = Portals.getConnectedPortal(portal.getId());
	//		if (target == null)
	//			return;

	//		entitiesPortalling.put(event.getPlayer().getUniqueId(), null);

	//		Location targetLocation = target.getSpawn().clone();
	//		Location playerLocation = event.getPlayer().getLocation();
	//		targetLocation.setPitch(playerLocation.getPitch());
	//		targetLocation.setYaw(playerLocation.getYaw());

	//		/* calculate new pitch, yaw and velocity */
	//		targetLocation = portal.getOrientation().rotateLocation(target.getOrientation(), targetLocation);
	//		Vector newVelocity = portal.getOrientation().rotateVector(target.getOrientation(), event.getPlayer().getVelocity());

	//		/* set new movement location */
	//		event.setTo(targetLocation);
	//		Log.info("portals", "teleport player " + event.getPlayer() + " from " + portal + " to " + target);

	//		/* retain velocity */
	//		event.getPlayer().setVelocity(newVelocity);
	//	} else {
	//		/* we just portalled */
	//		if (event.getFrom().getBlock().getType() == PortalsConfiguration.PORTAL_DEFAULT_MATERIAL_PORTAL_AREA_ACTIVATED)
	//			return;

	//		Location loc = entitiesPortalling.get(event.getPlayer().getUniqueId());
	//		if (loc == null) /* initial teleport */
	//			entitiesPortalling.put(event.getPlayer().getUniqueId(), event.getFrom().clone());
	//		else {
	//			/* 2 blocks away */
	//			if (event.getFrom().distance(loc) > 2.0)
	//				entitiesPortalling.remove(event.getPlayer().getUniqueId());
	//		}
	//	}
	//}
}
