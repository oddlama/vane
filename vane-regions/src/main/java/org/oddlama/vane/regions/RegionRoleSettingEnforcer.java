package org.oddlama.vane.regions;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
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

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_block_break(final BlockBreakEvent event) {
		// Prevent breaking of region blocks
		if (check_setting_at(event.getBlock(), event.getPlayer(), RoleSetting.BUILD, false)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_block_place(final BlockPlaceEvent event) {
		// Prevent (re-)placing of region blocks
		if (check_setting_at(event.getBlock(), event.getPlayer(), RoleSetting.BUILD, false)) {
			event.setCancelled(true);
		}
	}
}
