package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.oddlama.vane.util.LazyBlock;

public class RegionExtent {

	public static Object serialize(@NotNull final Object o) throws IOException {
		final var region_extent = (RegionExtent) o;
		final var json = new JSONObject();
		json.put("min", to_json(LazyBlock.class, region_extent.min));
		json.put("max", to_json(LazyBlock.class, region_extent.max));
		return json;
	}

	public static RegionExtent deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject) o;
		final var min = from_json(LazyBlock.class, json.get("min"));
		final var max = from_json(LazyBlock.class, json.get("max"));
		return new RegionExtent(min, max);
	}

	// Both inclusive, so we don't run into errors with
	// blocks outside the world (y<min_height || y>max_height).
	// Also, coordinates are sorted, so min is always the smaller coordinate on each axis.
	// For each x,y,z: min.[x,y,z] <= max.[x,y,z]
	private LazyBlock min; // inclusive
	private LazyBlock max; // inclusive

	public RegionExtent(final LazyBlock min, final LazyBlock max) {
		this.min = min;
		this.max = max;
	}

	public RegionExtent(final Block from, final Block to) {
		if (!from.getWorld().equals(to.getWorld())) {
			throw new RuntimeException("Invalid region extent across dimensions!");
		}

		// Sort coordinates along axes.
		this.min =
			new LazyBlock(
				from
					.getWorld()
					.getBlockAt(
						Math.min(from.getX(), to.getX()),
						Math.min(from.getY(), to.getY()),
						Math.min(from.getZ(), to.getZ())
					)
			);
		this.max =
			new LazyBlock(
				from
					.getWorld()
					.getBlockAt(
						Math.max(from.getX(), to.getX()),
						Math.max(from.getY(), to.getY()),
						Math.max(from.getZ(), to.getZ())
					)
			);
	}

	public UUID world() {
		return min.world_id();
	}

	public Block min() {
		return min.block();
	}

	public Block max() {
		return max.block();
	}

	public boolean is_inside(final Location loc) {
		if (!loc.getWorld().equals(min().getWorld())) {
			return false;
		}

		final var l = min();
		final var h = max();
		return (
			loc.getX() >= l.getX() &&
			loc.getX() < (h.getX() + 1) &&
			loc.getY() >= l.getY() &&
			loc.getY() < (h.getY() + 1) &&
			loc.getZ() >= l.getZ() &&
			loc.getZ() < (h.getZ() + 1)
		);
	}

	public boolean is_inside(final Block block) {
		if (!block.getWorld().equals(min().getWorld())) {
			return false;
		}

		final var l = min();
		final var h = max();
		return (
			block.getX() >= l.getX() &&
			block.getX() <= h.getX() &&
			block.getY() >= l.getY() &&
			block.getY() <= h.getY() &&
			block.getZ() >= l.getZ() &&
			block.getZ() <= h.getZ()
		);
	}

	public boolean intersects_extent(final RegionExtent other) {
		if (!min().getWorld().equals(other.min().getWorld())) {
			return false;
		}

		final var l1 = min();
		final var h1 = max();
		final var l2 = other.min();
		final var h2 = other.max();

		// Compute global min and max for each axis
		final var llx = Math.min(l1.getX(), l2.getX());
		final var lly = Math.min(l1.getY(), l2.getY());
		final var llz = Math.min(l1.getZ(), l2.getZ());
		final var hhx = Math.max(h1.getX(), h2.getX());
		final var hhy = Math.max(h1.getY(), h2.getY());
		final var hhz = Math.max(h1.getZ(), h2.getZ());

		// Compute global extent length
		final var extent_global_x = (hhx - llx) + 1;
		final var extent_global_y = (hhy - lly) + 1;
		final var extent_global_z = (hhz - llz) + 1;

		// Compute a sum of local extent lengths
		final var extent_sum_x = (h2.getX() - l2.getX()) + (h1.getX() - l1.getX()) + 2;
		final var extent_sum_y = (h2.getY() - l2.getY()) + (h1.getY() - l1.getY()) + 2;
		final var extent_sum_z = (h2.getZ() - l2.getZ()) + (h1.getZ() - l1.getZ()) + 2;

		// It intersects exactly when:
		//   for all and in axis: global_extent(a) < individual_extent_sum(a)
		return extent_global_x < extent_sum_x && extent_global_y < extent_sum_y && extent_global_z < extent_sum_z;
	}

	public boolean intersects_chunk(final Chunk chunk) {
		if (!chunk.getWorld().equals(min().getWorld())) {
			return false;
		}

		final var l1 = min();
		final var h1 = max();
		final var l2x = chunk.getX() * 16;
		final var l2z = chunk.getZ() * 16;
		final var h2x = (chunk.getX() + 1) * 16 - 1;
		final var h2z = (chunk.getZ() + 1) * 16 - 1;

		// Compute global min and max for each axis
		final var llx = Math.min(l1.getX(), l2x);
		final var llz = Math.min(l1.getZ(), l2z);
		final var hhx = Math.max(h1.getX(), h2x);
		final var hhz = Math.max(h1.getZ(), h2z);

		// Compute global extent length
		final var extent_global_x = (hhx - llx) + 1;
		final var extent_global_z = (hhz - llz) + 1;

		// Compute a sum of local extent lengths
		final var extent_sum_x = (h2x - l2x) + (h1.getX() - l1.getX()) + 2;
		final var extent_sum_z = (h2z - l2z) + (h1.getZ() - l1.getZ()) + 2;

		// It intersects exactly when:
		//   for all and in axis: global_extent(a) < individual_extent_sum(a)
		return extent_global_x < extent_sum_x && extent_global_z < extent_sum_z;
	}
}
