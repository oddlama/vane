package org.oddlama.vane.portals;

import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.portals.event.PortalConstructEvent;
import org.oddlama.vane.portals.event.PortalLinkConsoleEvent;
import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Plane;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.PortalBoundary;

public class PortalConstructor extends Listener<Portals> {

	@ConfigMaterial(def = Material.ENCHANTING_TABLE, desc = "The block used to build portal consoles.")
	public Material config_material_console;

	@ConfigMaterial(def = Material.OBSIDIAN, desc = "The block used to build the portal boundary. Variation 1.")
	public Material config_material_boundary_1;

	@ConfigMaterial(def = Material.CRYING_OBSIDIAN, desc = "The block used to build the portal boundary. Variation 2.")
	public Material config_material_boundary_2;

	@ConfigMaterial(def = Material.GOLD_BLOCK, desc = "The block used to build the portal boundary. Variation 3.")
	public Material config_material_boundary_3;

	@ConfigMaterial(
		def = Material.GILDED_BLACKSTONE,
		desc = "The block used to build the portal boundary. Variation 4."
	)
	public Material config_material_boundary_4;

	@ConfigMaterial(def = Material.EMERALD_BLOCK, desc = "The block used to build the portal boundary. Variation 5.")
	public Material config_material_boundary_5;

	@ConfigMaterial(def = Material.NETHERITE_BLOCK, desc = "The block used to build the portal origin.")
	public Material config_material_origin;

	@ConfigMaterial(def = Material.AIR, desc = "The block used to build the portal area.")
	public Material config_material_portal_area;

	@ConfigInt(def = 12, min = 1, desc = "Maximum horizontal distance between a console block and the portal.")
	public int config_console_max_distance_xz;

	@ConfigInt(def = 12, min = 1, desc = "Maximum vertical distance between a console block and the portal.")
	public int config_console_max_distance_y;

	@ConfigInt(
		def = 1024,
		min = 256,
		desc = "Maximum steps for the floodfill algorithm. This should only be increased if you want really big portals. It's recommended to keep this as low as possible."
	)
	public int config_area_floodfill_max_steps = 1024;

	@ConfigInt(def = 24, min = 8, desc = "Maximum portal area width (bounding box will be measured).")
	public int config_area_max_width;

	@ConfigInt(def = 24, min = 8, desc = "Maximum portal area height (bounding box will be measured).")
	public int config_area_max_height = 24;

	@ConfigInt(def = 128, min = 8, desc = "Maximum total amount of portal area blocks.")
	public int config_area_max_blocks = 128;

	@LangMessage
	public TranslatedMessage lang_select_boundary_now;

	@LangMessage
	public TranslatedMessage lang_console_invalid_type;

	@LangMessage
	public TranslatedMessage lang_console_different_world;

	@LangMessage
	public TranslatedMessage lang_console_too_far_away;

	@LangMessage
	public TranslatedMessage lang_console_linked;

	@LangMessage
	public TranslatedMessage lang_no_boundary_found;

	@LangMessage
	public TranslatedMessage lang_no_origin;

	@LangMessage
	public TranslatedMessage lang_multiple_origins;

	@LangMessage
	public TranslatedMessage lang_no_portal_block_above_origin;

	@LangMessage
	public TranslatedMessage lang_not_enough_portal_blocks_above_origin;

	@LangMessage
	public TranslatedMessage lang_too_large;

	@LangMessage
	public TranslatedMessage lang_too_small_spawn;

	@LangMessage
	public TranslatedMessage lang_too_many_portal_area_blocks;

	@LangMessage
	public TranslatedMessage lang_portal_area_obstructed;

	@LangMessage
	public TranslatedMessage lang_intersects_existing_portal;

	@LangMessage
	public TranslatedMessage lang_build_restricted;

	@LangMessage
	public TranslatedMessage lang_link_restricted;

	@LangMessage
	public TranslatedMessage lang_target_already_connected;

	@LangMessage
	public TranslatedMessage lang_source_use_restricted;

	@LangMessage
	public TranslatedMessage lang_target_use_restricted;

	private Set<Material> portal_boundary_build_materials = new HashSet<>();

	private HashMap<UUID, Block> pending_console = new HashMap<>();

	public PortalConstructor(Context<Portals> context) {
		super(context);
	}

	@Override
	public void on_config_change() {
		portal_boundary_build_materials.clear();
		portal_boundary_build_materials.add(config_material_boundary_1);
		portal_boundary_build_materials.add(config_material_boundary_2);
		portal_boundary_build_materials.add(config_material_boundary_3);
		portal_boundary_build_materials.add(config_material_boundary_4);
		portal_boundary_build_materials.add(config_material_boundary_5);
		portal_boundary_build_materials.add(config_material_origin);
	}

	public int max_dim_x(Plane plane) {
		return plane.x() ? config_area_max_width : 1;
	}

	public int max_dim_y(Plane plane) {
		return plane.y() ? config_area_max_height : 1;
	}

	public int max_dim_z(Plane plane) {
		return plane.z() ? config_area_max_width : 1;
	}

	private boolean remember_new_console(final Player player, final Block console_block) {
		final var changed = !console_block.equals(pending_console.get(player.getUniqueId()));
		// Add console_block as pending console
		pending_console.put(player.getUniqueId(), console_block);
		if (changed) {
			lang_select_boundary_now.send(player);
		}
		return changed;
	}

	private boolean can_link_console(
		final Player player,
		final PortalBoundary boundary,
		final Block console,
		boolean check_only
	) {
		return can_link_console(player, boundary.all_blocks(), console, null, check_only);
	}

	private boolean can_link_console(
		final Player player,
		final Portal portal,
		final Block console,
		boolean check_only
	) {
		// Gather all portal blocks that aren't consoles
		final var blocks = portal
			.blocks()
			.stream()
			.filter(pb -> pb.type() != PortalBlock.Type.CONSOLE)
			.map(pb -> pb.block())
			.collect(Collectors.toList());
		return can_link_console(player, blocks, console, portal, check_only);
	}

	private boolean can_link_console(
		final Player player,
		final List<Block> blocks,
		final Block console,
		@Nullable final Portal existing_portal,
		boolean check_only
	) {
		// Check a console block type
		if (console.getType() != config_material_console) {
			lang_console_invalid_type.send(player);
			return false;
		}

		// Check world
		if (!console.getWorld().equals(blocks.get(0).getWorld())) {
			lang_console_different_world.send(player);
			return false;
		}

		// Check distance
		boolean found_valid_block = false;
		for (final var block : blocks) {
			if (
				Math.abs(console.getX() - block.getX()) <= config_console_max_distance_xz &&
				Math.abs(console.getY() - block.getY()) <= config_console_max_distance_y &&
				Math.abs(console.getZ() - block.getZ()) <= config_console_max_distance_xz
			) {
				found_valid_block = true;
				break;
			}
		}

		if (!found_valid_block) {
			lang_console_too_far_away.send(player);
			return false;
		}

		// Call event
		final var event = new PortalLinkConsoleEvent(player, console, blocks, check_only, existing_portal);
		get_module().getServer().getPluginManager().callEvent(event);
		if (event.isCancelled() && !player.hasPermission(get_module().admin_permission)) {
			lang_link_restricted.send(player);
			return false;
		}

		return true;
	}

	private boolean link_console(final Player player, final Block console, final Portal portal) {
		if (!can_link_console(player, portal, console, false)) {
			return false;
		}

		// Add portal block
		get_module().add_new_portal_block(portal, create_portal_block(console));

		// Update block blocks
		portal.update_blocks(get_module());
		return true;
	}

	private PortalBoundary find_boundary(final Player player, final Block block) {
		final var boundary = PortalBoundary.search_at(this, block);
		if (boundary == null) {
			lang_no_boundary_found.send(player);
			return null;
		}

		// Check for error
		switch (boundary.error_state()) {
			case NONE:
				/* The Boundary is fine */break;
			case NO_ORIGIN:
				lang_no_origin.send(player);
				return null;
			case MULTIPLE_ORIGINS:
				lang_multiple_origins.send(player);
				return null;
			case NO_PORTAL_BLOCK_ABOVE_ORIGIN:
				lang_no_portal_block_above_origin.send(player);
				return null;
			case NOT_ENOUGH_PORTAL_BLOCKS_ABOVE_ORIGIN:
				lang_not_enough_portal_blocks_above_origin.send(player);
				return null;
			case TOO_LARGE_X:
				lang_too_large.send(player, "§6x");
				return null;
			case TOO_LARGE_Y:
				lang_too_large.send(player, "§6y");
				return null;
			case TOO_LARGE_Z:
				lang_too_large.send(player, "§6z");
				return null;
			case TOO_SMALL_SPAWN_X:
				lang_too_small_spawn.send(player, "§6x");
				return null;
			case TOO_SMALL_SPAWN_Y:
				lang_too_small_spawn.send(player, "§6y");
				return null;
			case TOO_SMALL_SPAWN_Z:
				lang_too_small_spawn.send(player, "§6z");
				return null;
			case PORTAL_AREA_OBSTRUCTED:
				lang_portal_area_obstructed.send(player);
				return null;
			case TOO_MANY_PORTAL_AREA_BLOCKS:
				lang_too_many_portal_area_blocks.send(
					player,
					"§6" + boundary.portal_area_blocks().size(),
					"§6" + config_area_max_blocks
				);
				return null;
		}

		if (boundary.intersects_existing_portal(this)) {
			lang_intersects_existing_portal.send(player);
			return null;
		}

		return boundary;
	}

	public boolean is_type_part_of_boundary(final Material material) {
		return (
			material == config_material_boundary_1 ||
			material == config_material_boundary_2 ||
			material == config_material_boundary_3 ||
			material == config_material_boundary_4 ||
			material == config_material_boundary_5
		);
	}

	public boolean is_type_part_of_boundary_or_origin(final Material material) {
		return material == config_material_origin || is_type_part_of_boundary(material);
	}

	private PortalBoundary check_construction_conditions(
		final Player player,
		final Block console,
		final Block boundary_block,
		boolean check_only
	) {
		if (get_module().is_portal_block(boundary_block)) {
			get_module()
				.log.severe(
					"construct_portal() was called on a boundary that already belongs to a portal! This is a bug."
				);
			return null;
		}

		// Search for valid portal boundary
		final var boundary = find_boundary(player, boundary_block);
		if (boundary == null) {
			return null;
		}

		// Check portal construct event
		final var event = new PortalConstructEvent(player, boundary, check_only);
		get_module().getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			lang_build_restricted.send(player);
			return null;
		}

		// Check console distance and build permission
		if (!can_link_console(player, boundary, console, true)) {
			return null;
		}

		return boundary;
	}

	private PortalBlock create_portal_block(final Block block) {
		final PortalBlock.Type type;
		var mat = block.getType();
		// treat cave air and void air as normal air
		if(mat == Material.CAVE_AIR || mat == Material.VOID_AIR) {
			mat = Material.AIR;
		}
		if (mat == config_material_console) {
			type = PortalBlock.Type.CONSOLE;
		} else if (mat == config_material_boundary_1) {
			type = PortalBlock.Type.BOUNDARY_1;
		} else if (mat == config_material_boundary_2) {
			type = PortalBlock.Type.BOUNDARY_2;
		} else if (mat == config_material_boundary_3) {
			type = PortalBlock.Type.BOUNDARY_3;
		} else if (mat == config_material_boundary_4) {
			type = PortalBlock.Type.BOUNDARY_4;
		} else if (mat == config_material_boundary_5) {
			type = PortalBlock.Type.BOUNDARY_5;
		} else if (mat == config_material_origin) {
			type = PortalBlock.Type.ORIGIN;
		} else if (mat == config_material_portal_area) {
			type = PortalBlock.Type.PORTAL;
		} else {
			get_module()
				.log.warning(
					"Invalid block type '" + mat + "' encountered in portal block creation. Assuming boundary variant 1."
				);
			type = PortalBlock.Type.BOUNDARY_1;
		}
		return new PortalBlock(block, type);
	}

	private boolean construct_portal(final Player player, final Block console, final Block boundary_block) {
		if (check_construction_conditions(player, console, boundary_block, true) == null) {
			return false;
		}

		// Show name chooser
		get_module()
			.menus.enter_name_menu.create(
				player,
				(p, name) -> {
					// Re-check conditions, as someone could have changed blocks. This prevents this race condition.
					final var boundary = check_construction_conditions(p, console, boundary_block, false);
					if (boundary == null) {
						return ClickResult.ERROR;
					}

					// Determine orientation
					final var orientation = Orientation.from(
						boundary.plane(),
						boundary.origin_block(),
						console,
						player.getLocation()
					);

					// Construct portal
					final var portal = new Portal(p.getUniqueId(), orientation, boundary.spawn());
					portal.name(name);
					get_module().add_new_portal(portal);

					// Add portal blocks
					for (final var block : boundary.all_blocks()) {
						get_module().add_new_portal_block(portal, create_portal_block(block));
					}

					// Link console
					link_console(p, console, portal);

					// Force update storage now, as a precaution.
					get_module().update_persistent_data();

					// Update portal blocks once
					portal.update_blocks(get_module());
					return ClickResult.SUCCESS;
				}
			)
			.open(player);

		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_interact_console(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final var block = event.getClickedBlock();
		if (block.getType() != config_material_console) {
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

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void on_player_interact_boundary(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		final var block = event.getClickedBlock();
		final var portal = get_module().portal_for(block);
		final var type = block.getType();
		if (portal == null && !portal_boundary_build_materials.contains(type)) {
			return;
		}

		// Break if no console is pending
		final var player = event.getPlayer();
		final var console = pending_console.remove(player.getUniqueId());
		if (console == null) {
			return;
		}

		if (portal == null) {
			if (construct_portal(player, console, block)) {
				swing_arm(player, event.getHand());
			}
		} else {
			if (link_console(player, console, portal)) {
				swing_arm(player, event.getHand());
			}
		}

		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);
	}
}
