package org.oddlama.vane.portals;

import org.oddlama.vane.portals.event.PortalSelectBoundaryEvent;
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
import org.oddlama.vane.core.module.Context;
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
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.util.Message;

public class PortalConstructor extends Listener<Portals> {
	// TODO test flint and steel to ignite a nether portal. this should be cancelled.
	// TODO materials group
	@ConfigMaterial(def = Material.ENCHANTING_TABLE, desc = "The block used to build portal consoles.")
	public Material config_material_console;
	@ConfigMaterial(def = Material.OBSIDIAN, desc = "The block used to build a portal boundary.")
	public Material config_material_boundary;
	@ConfigMaterial(def = Material.NETHERITE_BLOCK, desc = "The block used to build the portal origin.")
	public Material config_material_origin;

	@ConfigInt(def = 12, min = 1, desc = "Maximum horizontal distance between a console block and the portal.")
	public int config_console_max_distance_xz;
	@ConfigInt(def = 12, min = 1, desc = "Maximum vertical distance between a console block and the portal.")
	public int config_console_max_distance_y;

	@ConfigInt(def = 1024, min = 256, desc = "Maximum steps for the floodfill algorithm. This should only be increased if you want really big portals. It's recommended to keep this as low as possible.")
	public int config_area_floodfill_max_steps = 1024;
	@ConfigInt(def = 24, min = 8, desc = "Maximum portal area width (bounding box will be measured).")
	public int config_area_max_width;
	@ConfigInt(def = 24, min = 8, desc = "Maximum portal area height (bounding box will be measured).")
	public int config_area_max_height = 24;
	@ConfigInt(def = 64, min = 8, desc = "Maximum total amount of portal area blocks.")
	public int config_area_max_blocks = 64;

	@LangString public String lang_select_boundary_now;
	@LangString public String lang_console_too_far_away;
	@LangString public String lang_created_and_linked;
	@LangString public String lang_console_linked;

	@LangString public String lang_no_boundary_found;
	@LangString public String lang_no_origin;
	@LangString public String lang_multiple_origins;
	@LangString public String lang_no_portal_block_above_origin;
	@LangMessage public Message lang_too_large;
	@LangMessage public Message lang_too_small_spawn;
	@LangMessage public Message lang_too_many_portal_area_blocks;
	@LangString public String lang_portal_area_obstructed;
	@LangString public String lang_build_restricted;
	@LangString public String lang_intersects_existing_portal;

	@LangString public String lang_target_already_connected;
	@LangString public String lang_source_use_restricted;
	@LangString public String lang_target_use_restricted;

	private HashMap<UUID, Block> pending_console = new HashMap<>();

	public PortalConstructor(Context<Portals> context) {
		super(context);
	}

	public int max_dim_x(Plane plane) { return plane.x() ? config_area_max_width  : 1; }
	public int max_dim_y(Plane plane) { return plane.y() ? config_area_max_height : 1; }
	public int max_dim_z(Plane plane) { return plane.z() ? config_area_max_width  : 1; }

	private void remember_new_console(final Player player, final Block console_block) {
		// Add console_block as pending console
		pending_console.put(player.getUniqueId(), console_block);
		player.sendMessage(lang_select_boundary_now);
	}

	private boolean can_link_console(final PortalBoundary boundary, final Block console) {
		if (!console.getWorld().equals(boundary.origin_block().getWorld())) {
			return false;
		}

		for (final var block : boundary.all_blocks()) {
			if (Math.abs(console.getX() - block.getX()) <= config_console_max_distance_xz &&
			    Math.abs(console.getY() - block.getY()) <= config_console_max_distance_y &&
			    Math.abs(console.getZ() - block.getZ()) <= config_console_max_distance_xz) {
				return true;
			}
		}

		return false;
	}

	private PortalBoundary find_boundary(final Player player, final Block block) {
		final var boundary = PortalBoundary.search_at(this, block);
		if (boundary == null) {
			player.sendMessage(lang_no_boundary_found);
			return null;
		}

		// Check for error
		switch (boundary.error_state()) {
			case NONE: /* Boundary is fine */ break;
			case NO_ORIGIN:                    player.sendMessage(lang_no_origin);                    return null;
			case MULTIPLE_ORIGINS:             player.sendMessage(lang_multiple_origins);             return null;
			case NO_PORTAL_BLOCK_ABOVE_ORIGIN: player.sendMessage(lang_no_portal_block_above_origin); return null;
			case TOO_LARGE_X:                  player.sendMessage(lang_too_large.format("x"));        return null;
			case TOO_LARGE_Y:                  player.sendMessage(lang_too_large.format("y"));        return null;
			case TOO_LARGE_Z:                  player.sendMessage(lang_too_large.format("z"));        return null;
			case TOO_SMALL_SPAWN_X:            player.sendMessage(lang_too_small_spawn.format("x"));  return null;
			case TOO_SMALL_SPAWN_Y:            player.sendMessage(lang_too_small_spawn.format("y"));  return null;
			case TOO_SMALL_SPAWN_Z:            player.sendMessage(lang_too_small_spawn.format("z"));  return null;
			case TOO_MANY_PORTAL_AREA_BLOCKS:
				player.sendMessage(lang_too_many_portal_area_blocks.format(
					boundary.portal_area_blocks().size(),
					config_area_max_blocks));
				return null;
			case PORTAL_AREA_OBSTRUCTED:       player.sendMessage(lang_portal_area_obstructed);       return null;
		}

		if (boundary.intersects_existing_portal(this)) {
			player.sendMessage(lang_intersects_existing_portal);
			return null;
		}

		// Check portal select boundary event
		final var event = new PortalSelectBoundaryEvent(player, boundary);
		get_module().getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			player.sendMessage(lang_build_restricted);
			return null;
		}

		return boundary;
	}

	private boolean construct_portal(final Player player, final Block console, final Block boundary_block) {
		if (get_module().is_portal_block(boundary_block)) {
			get_module().log.severe("construct_portal() was called on a boundary that already belongs to a portal! This is a bug.");
			return false;
		}

		// Search for valid portal boundary
		final var test_boundary = find_boundary(player, boundary_block);
		if (test_boundary == null) {
			return false;
		}

		// Check console distance
		if (!can_link_console(test_boundary, console)) {
			player.sendMessage(lang_console_too_far_away);
			return true;
		}

		// Show name chooser
		//MenuFactory.createAnvilStringInputMenu(player, MENU_PORTAL_TITLE_NAME_PORTAL, Material.ENDER_PEARL, (menu, name) -> {
		//	menu.close();

		//	// Re-search for same boundary, as someone could have changed conditions, resulting in a race condition
		//	PortalBoundary boundary = find_boundary(player, block);
		//	if (boundary == null)
		//		return ClickResult.ERROR;

		//	// Determine orientation
		//	Orientation orientation = Orientation.getOrientation(boundary.getPlane(), boundary.getOriginBlock(), console, player.getLocation());

		//	// Check console distance
		//	if (!can_link_console(boundary, console)) {
		//		player.sendMessage(lang_console_too_far_away);
		//		return ClickResult.ERROR;
		//	}

		//	synchronized (criticalLock) {
		//		// Create database entries
		//		Portal portal = new Portal(name, orientation, boundary.getSpawn());
		//		portal.create();
		//		PortalBlock.createAllFromBoundary(portal.getId(), boundary);

		//		// Create dynmap marker
		//		PortalLayer.createMarker(portal);

		//		// Link console
		//		new PortalBlock(portal.getId(), console, PortalBlock.Type.CONSOLE).create();

		//		// Transmute blocks
		//		for (PortalBlock portalPortalBlock : PortalBlock.queryAllByPortalId(portal.getId()))
		//			portal.getStyle().getStyle().create(portal, portalPortalBlock);
		//	}

		//	player.sendMessage(lang_created_and_linked);
		//	return ClickResult.SUCCESS;
		//}).open();

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
		if (get_module().is_portal_block(block)) {
			return;
		}

		// TODO portal stone as item instead of shifting?
		// Only if player sneak-right-clicks the console
		final var player = event.getPlayer();
		if (!player.isSneaking() || event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		remember_new_console(player, block);
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
		if (type != config_build_material_boundary || type != Portals.MATERIAL_CONSOLE) {
			return;
		}

		// Break if no console is pending
		final var player = event.getPlayer();
		final var console = pending_console.remove(player.getUniqueId());
		if (console == null) {
			return;
		}

		final var portal = get_module().portal_for(block);
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
