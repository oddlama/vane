package org.oddlama.vane.regions;

import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.portals.event.PortalActivateEvent;
import org.oddlama.vane.portals.event.PortalChangeSettingsEvent;
import org.oddlama.vane.portals.event.PortalConstructEvent;
import org.oddlama.vane.portals.event.PortalDeactivateEvent;
import org.oddlama.vane.portals.event.PortalDestroyEvent;
import org.oddlama.vane.portals.event.PortalLinkConsoleEvent;
import org.oddlama.vane.portals.event.PortalOpenConsoleEvent;
import org.oddlama.vane.portals.event.PortalSelectTargetEvent;
import org.oddlama.vane.portals.event.PortalUnlinkConsoleEvent;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionRoleSettingEnforcer extends Listener<Regions> {
	public RegionRoleSettingEnforcer(Context<Regions> context) {
		super(context);
	}

	public boolean check_setting_at(final Location location, final Player player, final RoleSetting setting, final boolean check_against) {
		final var region = get_module().region_at(location);
		if (region == null) {
			return false;
		}

		final var group = region.region_group(get_module());
		return group.get_role(player.getUniqueId()).get_setting(setting) == check_against;
	}

	public boolean check_setting_at(final Block block, final Player player, final RoleSetting setting, final boolean check_against) {
		final var region = get_module().region_at(block);
		if (region == null) {
			return false;
		}

		final var group = region.region_group(get_module());
		return group.get_role(player.getUniqueId()).get_setting(setting) == check_against;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_block_break(final BlockBreakEvent event) {
		// Prevent breaking of region blocks
		if (check_setting_at(event.getBlock(), event.getPlayer(), RoleSetting.BUILD, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_block_place(final BlockPlaceEvent event) {
		// Prevent (re-)placing of region blocks
		if (check_setting_at(event.getBlock(), event.getPlayer(), RoleSetting.BUILD, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_entity_damage_by_entity(final EntityDamageByEntityEvent event) {
		final var damaged = event.getEntity();
		final var damager = event.getDamager();

		switch (damaged.getType()) {
			default:
				return;

			case ARMOR_STAND: {
				if (!(damager instanceof Player)) {
					break;
				}

				final var player_damager = (Player)damager;
				if (check_setting_at(damaged.getLocation().getBlock(), player_damager, RoleSetting.BUILD, false)) {
					event.setCancelled(true);
				}
				return;
			}

			case ITEM_FRAME: {
				if (!(damager instanceof Player)) {
					break;
				}

				final var player_damager = (Player)damager;
				final var item_frame = (ItemFrame)damaged;
				final var item = item_frame.getItem();
				if (item != null && item.getType() != Material.AIR) {
					// This is a player taking the item out of an item-frame
					if (check_setting_at(damaged.getLocation().getBlock(), player_damager, RoleSetting.CONTAINER, false)) {
						event.setCancelled(true);
					}
				}
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_hanging_break_by_entity(final HangingBreakByEntityEvent event) {
		final Entity remover = event.getRemover();
		Player player = null;

		if (remover instanceof Player) {
			player = (Player)remover;
		} else if (remover instanceof Projectile) {
			final var projectile = (Projectile)remover;
			final var shooter = projectile.getShooter();
			if (shooter instanceof Player) {
				player = (Player)shooter;
			}
		}

		if (player != null && check_setting_at(event.getEntity().getLocation(), player, RoleSetting.BUILD, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_hanging_place(final HangingPlaceEvent event) {
		if (check_setting_at(event.getEntity().getLocation(), event.getPlayer(), RoleSetting.BUILD, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_armor_stand_manipulate(final PlayerArmorStandManipulateEvent event) {
		if (check_setting_at(event.getRightClicked().getLocation(), event.getPlayer(), RoleSetting.BUILD, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_bucket_empty(final PlayerBucketEmptyEvent event) {
		if (check_setting_at(event.getBlockClicked(), event.getPlayer(), RoleSetting.BUILD, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_bucket_fill(final PlayerBucketFillEvent event) {
		if (check_setting_at(event.getBlockClicked(), event.getPlayer(), RoleSetting.BUILD, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_interact_entity(final PlayerInteractEntityEvent event) {
		final var entity = event.getRightClicked();
		if (entity == null || entity.getType() != EntityType.ITEM_FRAME) {
			return;
		}

		// Place or rotate item
		if (check_setting_at(entity.getLocation(), event.getPlayer(), RoleSetting.CONTAINER, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_interact(final PlayerInteractEvent event) {
		final var player = event.getPlayer();
		final var block = event.getClickedBlock();
		if (block == null) {
			return;
		}

		switch (event.getAction()) {
			default: return;
			case PHYSICAL: {
				if (Tag.PRESSURE_PLATES.isTagged(block.getType())) {
					if (check_setting_at(block, player, RoleSetting.USE, false)) {
						event.setCancelled(true);
					}
				} else if (block.getType() == Material.TRIPWIRE) {
					if (check_setting_at(block, player, RoleSetting.USE, false)) {
						event.setCancelled(true);
					}
				}
				return;
			}

			case RIGHT_CLICK_BLOCK: {
				if (check_setting_at(block, player, RoleSetting.USE, false)) {
					event.setCancelled(true);
				}
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_inventory_interact(final InventoryInteractEvent event) {
		final var clicker = event.getWhoClicked();
		if (!(clicker instanceof Player)) {
			return;
		}

		final var inventory = event.getInventory();
		if (inventory.getLocation() == null || inventory.getHolder() == null) {
			// Inventory is virtual / transient
			return;
		}

		final var holder = inventory.getHolder();
		if (holder instanceof Container || holder instanceof ChestedHorse || holder instanceof Minecart) {
			if (check_setting_at(inventory.getLocation(), (Player)clicker, RoleSetting.CONTAINER, false)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_activate(final PortalActivateEvent event) {
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_deactivate(final PortalDeactivateEvent event) {
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_construct(final PortalConstructEvent event) {
		// We have to check all blocks here, because otherwise players
		// could "steal" boundary blocks from unowned regions
		for (final var block : event.getBoundary().all_blocks()) {
			// Portals in regions may only be constructed by region administrators
			if (check_setting_at(block, event.getPlayer(), RoleSetting.ADMIN, false)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_destroy(final PortalDestroyEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		// We do NOT have to check all blocks here, because
		// an existing portal with its spawn inside a region
		// that the player controls can be considered proof of authority.
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			// Portals in regions may only be destroyed by region administrators
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void on_portal_link_console(final PortalLinkConsoleEvent event) {
		if (event.getPortal() != null && event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		if (get_module().region_at(event.getPortal().spawn()) != null) {
			// Portals in regions may be administrated by region administrators,
			// not only be the owner
			event.setCancelIfNotOwner(false);
		}

		// Portals in regions may only be administrated by region administrators
		// Check permission on console
		if (check_setting_at(event.getConsole(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			event.setCancelled(true);
			return;
		}

		// Check permission on portal if any
		if (event.getPortal() != null && check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void on_portal_unlink_console(final PortalUnlinkConsoleEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		if (get_module().region_at(event.getPortal().spawn()) != null) {
			// Portals in regions may be administrated by region administrators,
			// not only be the owner
			event.setCancelIfNotOwner(false);
		}

		// Portals in regions may only be administrated by region administrators
		// Check permission on console
		if (check_setting_at(event.getConsole(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			event.setCancelled(true);
			return;
		}

		// Check permission on portal
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_open_console(final PortalOpenConsoleEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		// Check permission on console
		if (check_setting_at(event.getConsole(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
			return;
		}

		// Check permission on portal
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_portal_select_target(final PortalSelectTargetEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		// Check permission on source portal
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
			return;
		}

		// Check permission on target portal
		if (event.getTarget() != null && check_setting_at(event.getTarget().spawn(), event.getPlayer(), RoleSetting.PORTAL, false)) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void on_portal_change_settings(final PortalChangeSettingsEvent event) {
		if (event.getPortal().owner().equals(event.getPlayer().getUniqueId())) {
			// Owner may always use their portals
			return;
		}

		if (get_module().region_at(event.getPortal().spawn()) == null) {
			return;
		}

		// Portals in regions may be administrated by region administrators,
		// not only be the owner
		event.setCancelIfNotOwner(false);

		// Now check if the player has the permission
		if (check_setting_at(event.getPortal().spawn(), event.getPlayer(), RoleSetting.ADMIN, false)) {
			event.setCancelled(true);
		}
	}
}
