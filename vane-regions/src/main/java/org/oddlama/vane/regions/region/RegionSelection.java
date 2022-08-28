package org.oddlama.vane.regions.region;

import static org.oddlama.vane.util.PlayerUtil.has_items;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

	public double price() {
		final var dx = 1 + Math.abs(primary.getX() - secondary.getX());
		final var dy = 1 + Math.abs(primary.getY() - secondary.getY());
		final var dz = 1 + Math.abs(primary.getZ() - secondary.getZ());
		final var cost =
			Math.pow(regions.config_cost_y_multiplicator, dy / 16.0) * regions.config_cost_xz_base / 256.0 * dx * dz;
		if (regions.config_economy_as_currency) {
			int decimal_places = regions.config_economy_decimal_places;
			if (decimal_places == -1) {
				decimal_places = regions.economy.fractionalDigits();
			}

			if (decimal_places >= 0) {
				return new BigDecimal(cost).setScale(decimal_places, RoundingMode.UP).doubleValue();
			} else {
				return cost;
			}
		} else {
			return Math.ceil(cost);
		}
	}

	public boolean can_afford(final Player player) {
		final var price = price();
		if (price <= 0) {
			return true;
		}

		if (regions.config_economy_as_currency) {
			return regions.economy.has(player, price);
		} else {
			final var map = new HashMap<ItemStack, Integer>();
			map.put(new ItemStack(regions.config_currency), (int) price);
			return has_items(player, map);
		}
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
		if (
			dx < regions.config_min_region_extent_x ||
			dy < regions.config_min_region_extent_y ||
			dz < regions.config_min_region_extent_z ||
			dx > regions.config_max_region_extent_x ||
			dy > regions.config_max_region_extent_y ||
			dz > regions.config_max_region_extent_z
		) {
			return false;
		}

		// Assert that it doesn't intersect an existing region
		if (intersects_existing()) {
			return false;
		}

		// Check that the player can afford it
		return can_afford(player);
	}

	public RegionExtent extent() {
		return new RegionExtent(primary, secondary);
	}
}
