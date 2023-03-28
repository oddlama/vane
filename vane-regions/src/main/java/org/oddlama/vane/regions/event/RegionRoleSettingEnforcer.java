package org.oddlama.vane.regions.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionRoleSettingEnforcer extends Listener<Regions> {

	public RegionRoleSettingEnforcer(Context<Regions> context) {
		super(context);
	}

	public boolean check_setting_at(
		final Location location,
		final Player player,
		final RoleSetting setting,
		final boolean check_against
	) {
		final var region = get_module().region_at(location);
		if (region == null) {
			return false;
		}

		final var group = region.region_group(get_module());
		return group.get_role(player.getUniqueId()).get_setting(setting) == check_against;
	}

	public boolean check_setting_at(
		final Block block,
		final Player player,
		final RoleSetting setting,
		final boolean check_against
	) {
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
			case ARMOR_STAND:
				{
					if (!(damager instanceof Player)) {
						break;
					}

					final var player_damager = (Player) damager;
					if (check_setting_at(damaged.getLocation().getBlock(), player_damager, RoleSetting.BUILD, false)) {
						event.setCancelled(true);
					}
					return;
				}
			case ITEM_FRAME:
				{
					if (!(damager instanceof Player)) {
						break;
					}

					final var player_damager = (Player) damager;
					final var item_frame = (ItemFrame) damaged;
					final var item = item_frame.getItem();
					if (item.getType() != Material.AIR) {
						// This is a player taking the item out of an item-frame
						if (
							check_setting_at(
								damaged.getLocation().getBlock(),
								player_damager,
								RoleSetting.CONTAINER,
								false
							)
						) {
							event.setCancelled(true);
						}
					}
				}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_hanging_break_by_entity(final HangingBreakByEntityEvent event) {
		final Entity remover = event.getRemover();
		Player player = null;

		if (remover instanceof Player) {
			player = (Player) remover;
		} else if (remover instanceof Projectile) {
			final var projectile = (Projectile) remover;
			final var shooter = projectile.getShooter();
			if (shooter instanceof Player) {
				player = (Player) shooter;
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
		if (entity.getType() != EntityType.ITEM_FRAME) {
			return;
		}

		// Place or rotate item
		if (check_setting_at(entity.getLocation(), event.getPlayer(), RoleSetting.CONTAINER, false)) {
			event.setCancelled(true);
		}
	}

	// The EventPriority is HIGH, so this is executed AFTER the portals try
	// to activate, which is a seperate permission.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_player_interact(final PlayerInteractEvent event) {
		final var player = event.getPlayer();
		final var block = event.getClickedBlock();
		if (block == null) {
			return;
		}

		switch (event.getAction()) {
			default:
				return;
			case PHYSICAL:
				{
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
			case RIGHT_CLICK_BLOCK:
				{
					if (check_setting_at(block, player, RoleSetting.USE, false)) {
						event.setCancelled(true);
					}
				}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_inventory_open(final InventoryOpenEvent event) {
		// Only relevant if viewing should be prohibited, too.
		if (!get_module().config_prohibit_viewing_containers) {
			return;
		}

		if (!(event.getPlayer() instanceof Player player)) {
			return;
		}

		final var inventory = event.getInventory();
		if (inventory.getLocation() == null || inventory.getHolder() == null) {
			// Inventory is virtual / transient
			return;
		}

		final var holder = inventory.getHolder();
		if (holder instanceof DoubleChest || holder instanceof Container || holder instanceof Minecart) {
			if (check_setting_at(inventory.getLocation(), player, RoleSetting.CONTAINER, false)) {
				event.setCancelled(true);
			}
		}
	}

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
		if (holder instanceof DoubleChest || holder instanceof Container || holder instanceof Minecart) {
			if (check_setting_at(inventory.getLocation(), (Player) clicker, RoleSetting.CONTAINER, false)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_inventory_click(final InventoryClickEvent event) {
		on_player_inventory_interact(event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_inventory_drag(final InventoryDragEvent event) {
		on_player_inventory_interact(event);
	}
}
