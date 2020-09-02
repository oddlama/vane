package org.oddlama.vane.util;

import static org.oddlama.vane.util.MaterialUtil.is_replaceable_grass;
import static org.oddlama.vane.util.MaterialUtil.is_tillable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import java.util.Comparator;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;

public class BlockUtil {
	public static final List<BlockFace> BLOCK_FACES = Arrays.asList(
	    BlockFace.NORTH,
	    BlockFace.EAST,
	    BlockFace.SOUTH,
	    BlockFace.WEST,
	    BlockFace.UP,
	    BlockFace.DOWN);

	public static final List<BlockFace> XZ_FACES = Arrays.asList(
	    BlockFace.NORTH,
	    BlockFace.EAST,
	    BlockFace.SOUTH,
	    BlockFace.WEST);

	public static final int NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX = 6;
	public static final List<List<BlockVector>> NEAREST_RELATIVE_BLOCKS_FOR_RADIUS = new ArrayList<>();
	static {
		for (int i = 0; i <= NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX; ++i) {
			NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.add(nearest_blocks_for_radius(i));
		}
	}

	public static boolean equals_pos(final Block b1, final Block b2) {
		return b1.getX() == b2.getX() && b1.getY() == b2.getY() && b1.getZ() == b2.getZ();
	}

	public static void drop_naturally(Block block, ItemStack drop) {
		drop_naturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
	}

	public static void drop_naturally(Location loc, ItemStack drop) {
		loc.getWorld().dropItemNaturally(loc, drop);
	}

	public static List<BlockVector> nearest_blocks_for_radius(int radius) {
		final var ret = new ArrayList<BlockVector>();

		// Use square bounding box
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				// Only circular area
				if (x * x + z * z > radius * radius + 0.5) {
					continue;
				}

				ret.add(new BlockVector(x, 0, z));
			}
		}

		Collections.sort(ret, new BlockVectorRadiusComparator());
		return ret;
	}

	public static @NotNull Block relative(@NotNull final Block block, @NotNull final Vector relative) {
		return block.getRelative(relative.getBlockX(), relative.getBlockY(), relative.getBlockZ());
	}

	public static Block next_tillable_block(final Block root_block, int radius, boolean careless) {
		for (final var relative_pos : NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.get(radius)) {
			final var block = relative(root_block, relative_pos);

			// Check for a tillable material
			if (!is_tillable(block.getType())) {
				continue;
			}

			// Get block above
			final var above = block.getRelative(0, 1, 0);
			if (above == null) {
				continue;
			}

			if (above.getType() == Material.AIR) {
				// If the block above is air, we can till the block.
				return block;
			} else if (careless && is_replaceable_grass(above.getType())) {
				// If the item has the careless enchantment, and the block above
				// is replaceable grass, delete it and return the block.
				above.setType(Material.AIR);
				return block;
			}
		}

		// We are outside of the radius.
		return null;
	}

	public static Block next_seedable_block(final Block root_block, Material farmland_type, int radius) {
		for (var relative_pos : NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.get(radius)) {
			final var block = relative(root_block, relative_pos);
			final var below = block.getRelative(BlockFace.DOWN);

			// Block below must be farmland and the block itself must be air
			if (below.getType() == farmland_type && block.getType() == Material.AIR) {
				return block;
			}
		}

		// We are outside of the radius.
		return null;
	}

	public static class BlockVectorRadiusComparator implements Comparator<BlockVector> {
		@Override
		public int compare(BlockVector a, BlockVector b) {
			return (a.getBlockX() * a.getBlockX() + a.getBlockY() * a.getBlockY() + a.getBlockZ() * a.getBlockZ())
			     - (b.getBlockX() * b.getBlockX() + b.getBlockY() * b.getBlockY() + b.getBlockZ() * b.getBlockZ());
		}
	}
}

