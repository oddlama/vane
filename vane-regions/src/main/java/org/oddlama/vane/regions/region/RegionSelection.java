package org.oddlama.vane.regions.region;

import static org.oddlama.vane.util.PlayerUtil.has_items;

import java.util.HashMap;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.regions.Regions;

public class RegionSelection {
	private Regions regions;
	public Block primary = null;
	public Block secondary = null;

	public RegionSelection(final Regions regions) {
		this.regions = regions;
	}

	public boolean intersects_existing() {
		final var extent = extent();
		for (final var r : regions.all_regions()) {
			if (!r.extent().min().getWorld().equals(primary.getWorld())) {
				continue;
			}

			if (extent.intersects_extent(r.extent())) {
				return true;
			}
		}

		return false;
	}

	public int price() {
		final var dx = 1 + Math.abs(primary.getX() - secondary.getX());
		final var dy = 1 + Math.abs(primary.getY() - secondary.getY());
		final var dz = 1 + Math.abs(primary.getZ() - secondary.getZ());
		return (int)Math.ceil(Math.pow(regions.config_cost_y_multiplicator, dy / 16.0) * regions.config_cost_xz_base / 256.0 * dx * dz);
	}

	public boolean can_afford(final Player player) {
		final var price = price();
		if (price <= 0) {
			return true;
		}
		final var map = new HashMap<ItemStack, Integer>();
		map.put(new ItemStack(regions.config_currency), price);
		return has_items(player, map);
	}

	public boolean is_valid(final Player player) {
		// Both blocks set
		if (primary == null || secondary == null) {
			return false;
		}

		// Worlds match
		if (!primary.getWorld().equals(secondary.getWorld())) {
			return false;
		}

		final var dx = 1 + Math.abs(primary.getX() - secondary.getX());
		final var dy = 1 + Math.abs(primary.getY() - secondary.getY());
		final var dz = 1 + Math.abs(primary.getZ() - secondary.getZ());

		// min <= extent <= max
		if (dx < regions.config_min_region_extent_x ||
			dy < regions.config_min_region_extent_y ||
			dz < regions.config_min_region_extent_z ||
			dx > regions.config_max_region_extent_x ||
			dy > regions.config_max_region_extent_y ||
			dz > regions.config_max_region_extent_z) {
			return false;
		}

		// Assert that it doesn't intersect an existing region
		if (intersects_existing()) {
			return false;
		}

		// Check that the player can afford it
		if (!can_afford(player)) {
			return false;
		}

		return true;
	}

	public RegionExtent extent() {
		return new RegionExtent(primary, secondary);
	}
}
