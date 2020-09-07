package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
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
import org.oddlama.vane.core.Listener;

public class PortalConstructor extends Listener<Portals> {
	public static final Material MATERIAL_BUILD_BOUNDARY = Material.OBSIDIAN;
	// TODO custom origin block?
	public static final Material MATERIAL_BUILD_ORIGIN = Material.NETHERITE_BLOCK;

	private Portals portals;
	private HashMap<UUID, Block> pending_console = new HashMap<>();

	public PortalConstructor(Portals portals) {
		super(portals);
		this.portals = portals;
	}

	private void begin_portal_construction(final Player player, final Block console_block) {
		// Add console_block as pending console
		pending_console.put(player.getUniqueId(), console_block);
		// TODO player.sendMessage(Portals.PORTAL_SELECT_BOUNDARY_NOW.get());
		player.sendMessage("click boundary");
	}

	private boolean can_link_console(Collection<Block> boundary_blocks, Block origin_block, Block console) {
		if (!console.getWorld().equals(origin_block.getWorld())) {
			return false;
		}

		for (Block block : boundary_blocks) {
			if (Math.abs(console.getX() - block.getX()) <= Portals.PORTAL_CONSOLE_MAX_DISTANCE_TO_BOUNDARY_XZ &&
			    Math.abs(console.getY() - block.getY()) <= Portals.PORTAL_CONSOLE_MAX_DISTANCE_TO_BOUNDARY_Y &&
			    Math.abs(console.getZ() - block.getZ()) <= Portals.PORTAL_CONSOLE_MAX_DISTANCE_TO_BOUNDARY_XZ) {
					return true;
				}
		}

		if (Math.abs(console.getX() - origin_block.getX()) <= Portals.PORTAL_CONSOLE_MAX_DISTANCE_TO_BOUNDARY_XZ &&
		    Math.abs(console.getY() - origin_block.getY()) <= Portals.PORTAL_CONSOLE_MAX_DISTANCE_TO_BOUNDARY_Y &&
		    Math.abs(console.getZ() - origin_block.getZ()) <= Portals.PORTAL_CONSOLE_MAX_DISTANCE_TO_BOUNDARY_XZ) {
				return true;
			}
		return false;
	}

		/*
	private PortalBoundary find_boundary(final Player player, final Block block) {
		final var maxAreaBlocks = isAdmin ? Portals.PORTAL_AREA_MAX_BLOCKS_ADMIN : Portals.PORTAL_AREA_MAX_BLOCKS;

		PortalBoundary boundary = PortalBoundary.searchAt(block, maxAreaBlocks);
		if (boundary == null) {
			player.sendMessage(Portals.PORTAL_NO_BOUNDARY_FOUND.get());
			return null;
		}

		switch (boundary.getErrorState()) {
			case NONE:
				// Boundary is fine
				break;

			case TOO_MANY_PORTAL_AREA_BLOCKS:
				player.sendMessage(boundary.getErrorState().getErrorMessage().replace("%maxAreaBlocks%", Integer.toString(maxAreaBlocks)));
				return null;

			default:
				player.sendMessage(boundary.getErrorState().getErrorMessage());
				return null;
		}

		if (boundary.intersectsExistingPortal()) {
			player.sendMessage(Portals.PORTAL_INTERSECTS_EXISTING_PORTAL.get());
			return null;
		}

		if (Regions.isRestricted(player, boundary.getOriginBlock(), UserFlag.EDIT_PORTALS)) {
			player.sendMessage(Portals.PORTAL_RESTRICTED_IN_REGION.get());
			return null;
		}

		// FIXME note: in theory, player permissions have to be checked on all involved blocks (build and edit portal)
		// to make sure that someone does not use existing structures to build a portal
		return boundary;
	}
		*/

	private boolean construct_portal(final Player player, final Block console, final Block boundary_block) {
		if (portals.is_portal_block(boundary_block)) {
			get_module().log.severe("construct_portal() was called on a boundary that already belongs to a portal! This is a bug.");
			return false;
		}

		/*
		// Search for valid portal boundary
		final var test_boundary = find_boundary(player, block);
		if (test_boundary == null) {
			return false;
		}

		// Check console distance
		if (!can_link_console(test_boundary.getBoundaryBlocks(), test_boundary.getOriginBlock(), console)) {
			player.sendMessage(Portals.PORTAL_CONSOLE_TOO_FAR_AWAY.get());
			return true;
		}

		// Show name chooser
		MenuFactory.createAnvilStringInputMenu(player, Portals.MENU_PORTAL_TITLE_NAME_PORTAL, Material.ENDER_PEARL, (menu, name) -> {
			menu.close();

			// Re-search for same boundary, as someone could have changed conditions, resulting in a race condition
			PortalBoundary boundary = find_boundary(player, block);
			if (boundary == null)
				return ClickResult.ERROR;

			// Determine orientation
			Orientation orientation = Orientation.getOrientation(boundary.getPlane(), boundary.getOriginBlock(), console, player.getLocation());

			// Check console distance
			if (!can_link_console(boundary.getBoundaryBlocks(), boundary.getOriginBlock(), console)) {
				player.sendMessage(Portals.PORTAL_CONSOLE_TOO_FAR_AWAY.get());
				return ClickResult.ERROR;
			}

			synchronized (criticalLock) {
				// Create database entries
				Portal portal = new Portal(name, orientation, boundary.getSpawn());
				portal.create();
				PortalBlock.createAllFromBoundary(portal.getId(), boundary);

				// Create dynmap marker
				PortalLayer.createMarker(portal);

				// Link console
				new PortalBlock(portal.getId(), console, PortalBlock.Type.CONSOLE).create();

				// Transmute blocks
				for (PortalBlock portalPortalBlock : PortalBlock.queryAllByPortalId(portal.getId()))
					portal.getStyle().getStyle().create(portal, portalPortalBlock);
			}

			player.sendMessage(Portals.PORTAL_CREATED_AND_LINKED.get());
			return ClickResult.SUCCESS;
		}).open();
		*/

		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_interact_construct_portal(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final var block = event.getClickedBlock();
		if (block.getType() != Portals.MATERIAL_CONSOLE) {
			return;
		}

		// Abort if the console belongs to another portal already.
		if (portals.is_portal_block(block)) {
			return;
		}

		// TODO portal stone as item instead of shifting?
		// Only if player sneak-right-clicks the console
		final var player = event.getPlayer();
		if (!player.isSneaking() || event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		begin_portal_construction(player, block);
		swing_arm(player, event.getHand());
		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_interact_boundary(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final var block = event.getClickedBlock();
		final var type = block.getType();
		if (type != MATERIAL_BUILD_BOUNDARY || type != Portals.MATERIAL_CONSOLE) {
			return;
		}

		// Break if no console is pending
		final var player = event.getPlayer();
		final var console = pending_console.remove(player.getUniqueId());
		if (console == null) {
			return;
		}

		final var portal = portals.portal_for(block);
		if (portal == null) {
			if (construct_portal(player, console, block)) {
				swing_arm(player, event.getHand());
			}
		} else {
			if (portal.link_console(player, console, block)) {
				swing_arm(player, event.getHand());
			}
		}

		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);
	}
}
