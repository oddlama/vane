package org.oddlama.vane.regions.event;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;
import org.oddlama.vane.regions.region.EnvironmentSetting;

public class RegionEnvironmentSettingEnforcer extends Listener<Regions> {

	public RegionEnvironmentSettingEnforcer(Context<Regions> context) {
		super(context);
	}

	public boolean check_setting_at(
		final Location location,
		final EnvironmentSetting setting,
		final boolean check_against
	) {
		final var region = get_module().region_at(location);
		if (region == null) {
			return false;
		}

		final var group = region.region_group(get_module());
		return group.get_setting(setting) == check_against;
	}

	public boolean check_setting_at(final Block block, final EnvironmentSetting setting, final boolean check_against) {
		final var region = get_module().region_at(block);
		if (region == null) {
			return false;
		}

		final var group = region.region_group(get_module());
		return group.get_setting(setting) == check_against;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_block_explode(final BlockExplodeEvent event) {
		// Prevent explosions from removing region blocks
		event.blockList().removeIf(block -> check_setting_at(block, EnvironmentSetting.EXPLOSIONS, false));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_entity_explode(final EntityExplodeEvent event) {
		// Prevent explosions from removing region blocks
		event.blockList().removeIf(block -> check_setting_at(block, EnvironmentSetting.EXPLOSIONS, false));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_entity_change_block(final EntityChangeBlockEvent event) {
		if (!(event.getEntity() instanceof Monster)) {
			return;
		}

		// Prevent monster entities from changing region blocks
		if (check_setting_at(event.getBlock(), EnvironmentSetting.MONSTERS, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_block_burn(final BlockBurnEvent event) {
		if (check_setting_at(event.getBlock(), EnvironmentSetting.FIRE, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_block_spread(final BlockSpreadEvent event) {
		EnvironmentSetting setting;
		switch (event.getNewState().getType()) {
			default:
				return;
			case FIRE:
				setting = EnvironmentSetting.FIRE;
				break;
			case VINE:
				setting = EnvironmentSetting.VINE_GROWTH;
				break;
		}

		if (check_setting_at(event.getBlock(), setting, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_creature_spawn(final CreatureSpawnEvent event) {
		// Only cancel natural spawns and alike
		switch (event.getSpawnReason()) {
			case JOCKEY:
			case MOUNT:
			case NATURAL:
				break;
			default:
				return;
		}

		final var entity = event.getEntity();
		if (entity instanceof Monster) {
			if (check_setting_at(event.getLocation(), EnvironmentSetting.MONSTERS, false)) {
				event.setCancelled(true);
			}
		} else if (entity instanceof Animals) {
			if (check_setting_at(event.getLocation(), EnvironmentSetting.ANIMALS, false)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_entity_damage_by_entity(final EntityDamageByEntityEvent event) {
		final var damaged = event.getEntity();
		final var damager = event.getDamager();

		if (damaged.getType() != EntityType.PLAYER) {
			return;
		}

		final Player player_damaged = (Player) damaged;
		final Player player_damager;
		if (damager instanceof Player) {
			player_damager = (Player) damager;
		} else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
			player_damager = (Player) ((Projectile) damager).getShooter();
		} else {
			return;
		}

		if (
			player_damager != null &&
			player_damaged != player_damager &&
			(
				check_setting_at(player_damaged.getLocation(), EnvironmentSetting.PVP, false) ||
				check_setting_at(player_damager.getLocation(), EnvironmentSetting.PVP, false)
			)
		) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_hanging_break_event(final HangingBreakEvent event) {
		switch (event.getCause()) {
			default:
				return;
			case ENTITY:
				return; // Handeled by on_hanging_break_by_entity
			case EXPLOSION:
				{
					if (check_setting_at(event.getEntity().getLocation(), EnvironmentSetting.EXPLOSIONS, false)) {
						event.setCancelled(true);
					}
				}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_player_interact(final PlayerInteractEvent event) {
		if (event.getAction() != Action.PHYSICAL) {
			return;
		}

		final var block = event.getClickedBlock();
		if (block != null && block.getType() == Material.FARMLAND) {
			if (check_setting_at(block, EnvironmentSetting.TRAMPLE, false)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_potion_splash(final PotionSplashEvent event) {
		// Only if a player threw the potion check for PVP
		if (!(event.getEntity().getShooter() instanceof Player)) {
			return;
		}

		final var thrower = (Player) event.getEntity().getShooter();
		final var source_pvp_restricted = check_setting_at(thrower.getLocation(), EnvironmentSetting.PVP, false);

		// Cancel all damage to players if either thrower or damaged is
		// inside no-PVP region
		for (final var target : event.getAffectedEntities()) {
			if (!(target instanceof Player)) {
				continue;
			}

			if (source_pvp_restricted || check_setting_at(target.getLocation(), EnvironmentSetting.PVP, false)) {
				event.setIntensity(target, 0);
				return;
			}
		}
	}
}
