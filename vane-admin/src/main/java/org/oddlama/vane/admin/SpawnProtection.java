package org.oddlama.vane.admin;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigString;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class SpawnProtection extends Listener<Admin> {

	private static final String PERMISSION_NAME = "vane.admin.bypass_spawn_protection";
	private Permission permission = new Permission(
		PERMISSION_NAME,
		"Allow player to bypass spawn protection",
		PermissionDefault.OP
	);

	@ConfigBoolean(def = true, desc = "Allow interaction events at spawn (buttons, levers, etc.).")
	private boolean config_allow_interaction;

	@ConfigInt(def = 64, min = 0, desc = "Radius to protect.")
	private int config_radius;

	@ConfigString(def = "world", desc = "The spawn world.")
	private String config_world;

	@ConfigBoolean(def = true, desc = "Use world's spawn location instead of the specified center coordinates.")
	private boolean config_use_spawn_location;

	@ConfigInt(def = 0, desc = "Center X coordinate.")
	private int config_x;

	@ConfigInt(def = 0, desc = "Center Z coordinate.")
	private int config_z;

	public SpawnProtection(Context<Admin> context) {
		super(
			context.group_default_disabled(
				"spawn_protection",
				"Enable spawn protection. Slightly more sophisticated than the vanilla spawn protection, if you need even more control, use regions. This will prevent anyone from modifying the spawn of the world if they don't have the permission '" +
				PERMISSION_NAME +
				"'."
			)
		);
		get_module().register_permission(permission);
	}

	private Location spawn_center = null;

	@Override
	public void on_config_change() {
		spawn_center = null;
		schedule_next_tick(() -> {
			final var world = get_module().getServer().getWorld(config_world);
			if (world == null) {
				//todo print error and show valid worlds.
				get_module()
					.log.warning(
						"The world \"" + config_world + "\" configured for spawn-protection could not be found."
					);
				get_module().log.warning("These are the names of worlds existing on this server:");
				for (final var w : get_module().getServer().getWorlds()) {
					get_module().log.warning("  \"" + w.getName() + "\"");
				}
				spawn_center = null;
			} else {
				if (config_use_spawn_location) {
					spawn_center = world.getSpawnLocation();
					spawn_center.setY(0);
				} else {
					spawn_center = new Location(world, config_x, 0, config_z);
				}
			}
		});
	}

	public boolean deny_modify_spawn(final Block block, final Entity entity) {
		return deny_modify_spawn(block.getLocation(), entity);
	}

	public boolean deny_modify_spawn(final Location location, final Entity entity) {
		if (spawn_center == null || !(entity instanceof Player)) {
			return false;
		}

		final var dx = location.getX() - spawn_center.getX();
		final var dz = location.getZ() - spawn_center.getZ();
		final var distance = Math.sqrt(dx * dx + dz * dz);
		if (distance > config_radius) {
			return false;
		}

		return !entity.hasPermission(permission);
	}

	/* ************************ blocks ************************ */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_block_break(BlockBreakEvent event) {
		if (deny_modify_spawn(event.getBlock(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_block_place(BlockPlaceEvent event) {
		if (deny_modify_spawn(event.getBlock(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	/* ************************ hanging ************************ */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_hanging_break_by_entity(HangingBreakByEntityEvent event) {
		if (deny_modify_spawn(event.getEntity().getLocation(), event.getRemover())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_hanging_place(HangingPlaceEvent event) {
		if (deny_modify_spawn(event.getEntity().getLocation(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	/* ************************ player ************************ */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_armor_stand_manipulate(PlayerArmorStandManipulateEvent event) {
		if (deny_modify_spawn(event.getRightClicked().getLocation(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_bucket_empty(PlayerBucketEmptyEvent event) {
		if (deny_modify_spawn(event.getBlock(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_bucket_fill(PlayerBucketFillEvent event) {
		if (deny_modify_spawn(event.getBlock(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_interact_entity(PlayerInteractEntityEvent event) {
		if (!config_allow_interaction && deny_modify_spawn(event.getRightClicked().getLocation(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_interact(PlayerInteractEvent event) {
		if (
			event.getClickedBlock() != null &&
			!config_allow_interaction &&
			deny_modify_spawn(event.getClickedBlock(), event.getPlayer())
		) {
			event.setCancelled(true);
		}
	}
}
