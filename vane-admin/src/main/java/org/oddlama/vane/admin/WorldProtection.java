package org.oddlama.vane.admin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.Listener;

public class WorldProtection extends Listener<Admin> {
	private static final String PERMISSION_NAME = "vane.admin.modify_world";
	private Permission permission = new Permission(PERMISSION_NAME, "Allow player to modify world", PermissionDefault.FALSE);

	public WorldProtection(Context<Admin> context) {
		super(context.group("world_protection", "Enable world protection. This will prevent anyone from modyfing the world of they don't have the permission '" + PERMISSION_NAME + "'."));
		get_module().register_permission(permission);
	}

	public boolean deny_modify_world(final Entity entity) {
		if (entity == null || !(entity instanceof Player)) {
			return false;
		}

		return !((Player)entity).hasPermission(permission);
	}

	/* ************************ blocks ************************ */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_block_break(BlockBreakEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_block_place(BlockPlaceEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	/* ************************ enchantment ************************ */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_prepare_item_enchant(PrepareItemEnchantEvent event) {
		if (deny_modify_world(event.getEnchanter())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_item_enchant(EnchantItemEvent event) {
		if (deny_modify_world(event.getEnchanter())) {
			event.setCancelled(true);
		}
	}

	/* ************************ entity ************************ */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_entity_combust_by_entity(EntityCombustByEntityEvent event) {
		if (deny_modify_world(event.getCombuster())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_entity_damage(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent) {
			final var damage_event = (EntityDamageByEntityEvent)event;
			if (deny_modify_world(damage_event.getDamager())) {
				event.setCancelled(true);
			} else if (deny_modify_world(damage_event.getEntity())) {
				event.setCancelled(true);
			}
		} else if (deny_modify_world(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_entity_food_level_change(FoodLevelChangeEvent event) {
		if (deny_modify_world(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	/* ************************ hanging ************************ */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_hanging_break_by_entity(HangingBreakByEntityEvent event) {
		if (deny_modify_world(event.getRemover())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_hanging_place(HangingPlaceEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	/* ************************ inventory ************************ */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_item_craft(CraftItemEvent event) {
		if (deny_modify_world(event.getWhoClicked())) {
			event.setCancelled(true);
		}
	}

	/* ************************ player ************************ */

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_armor_stand_manipulate(PlayerArmorStandManipulateEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_bucket_empty(PlayerBucketEmptyEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_bucket_fill(PlayerBucketFillEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_edit_book(PlayerEditBookEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_interact_entity(PlayerInteractEntityEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_interact(PlayerInteractEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_shear_entity(PlayerShearEntityEvent event) {
		if (deny_modify_world(event.getPlayer())) {
			event.setCancelled(true);
		}
	}
}
