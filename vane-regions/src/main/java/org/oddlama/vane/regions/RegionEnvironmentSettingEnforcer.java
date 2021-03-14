package org.oddlama.vane.regions;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.region.EnvironmentSetting;

import org.bukkit.block.Block;
import org.bukkit.Location;

public class RegionEnvironmentSettingEnforcer extends Listener<Regions> {
	public RegionEnvironmentSettingEnforcer(Context<Regions> context) {
		super(context);
	}

	public boolean check_setting_at(final Location location, final EnvironmentSetting setting, final boolean check_against) {
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

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_entity_explode(final EntityExplodeEvent event) {
		// Prevent explosions from removing region blocks
		final var it = event.blockList().iterator();
		while (it.hasNext()) {
			if (check_setting_at(it.next(), EnvironmentSetting.EXPLOSIONS, false)) {
				it.remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_entity_change_block(final EntityChangeBlockEvent event) {
		// Prevent entities from changing region blocks
		if (check_setting_at(event.getBlock(), EnvironmentSetting.MONSTERS, false)) {
			event.setCancelled(true);
		}
	}
}
