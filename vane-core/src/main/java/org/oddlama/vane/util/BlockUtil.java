package org.oddlama.vane.util;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import static org.oddlama.vane.util.MaterialUtil.is_tillable;
import static org.oddlama.vane.util.MaterialUtil.is_replaceable_grass;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.inventory.ItemStack;

public class BlockUtil {
	public static final BlockFace[] BLOCK_FACES = new BlockFace[] {
	    BlockFace.NORTH,
	    BlockFace.EAST,
	    BlockFace.SOUTH,
	    BlockFace.WEST,
	    BlockFace.UP,
	    BlockFace.DOWN};

	public static final BlockFace[] XZ_FACES = new BlockFace[] {
	    BlockFace.NORTH,
	    BlockFace.EAST,
	    BlockFace.SOUTH,
	    BlockFace.WEST};

	public static List<List<BlockPosition>> NEAREST_RELATIVE_BLOCKS_FOR_RADIUS = new ArrayList<>();
	static {
		for (int i = 0; i < 5; ++i) {
			NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.add(nearest_blocks_for_radius(i + 1));
		}
	}

	public static boolean equals_pos(final Block b1, final Block b2) {
		return b1.getX() == b2.getX() && b1.getY() == b2.getY() && b1.getZ() == b2.getZ();
	}

	public static void harvest_plant(Block block) {
		ItemStack[] drops = null;
		switch (block.getType()) {
			default:
				return;

			case WHEAT:       drops = new ItemStack[] {new ItemStack(Material.WHEAT,       1 + (int)(Math.random() * 2.5))}; break;
			case CARROTS:     drops = new ItemStack[] {new ItemStack(Material.CARROT,      1 + (int)(Math.random() * 2.5))}; break;
			case POTATOES:    drops = new ItemStack[] {new ItemStack(Material.POTATO,      1 + (int)(Math.random() * 2.5))}; break;
			case BEETROOTS:   drops = new ItemStack[] {new ItemStack(Material.BEETROOT,    1 + (int)(Math.random() * 2.5))}; break;
			case NETHER_WART: drops = new ItemStack[] {new ItemStack(Material.NETHER_WART, 1 + (int)(Math.random() * 2.5))}; break;
		}

		if (!(block.getBlockData() instanceof Ageable)) {
			return;
		}

		// Only harvest fully grown plants
		var ageable = (Ageable)block.getBlockData();
		if (ageable.getAge() != ageable.getMaximumAge()) {
			return;
		}

		// Fire event
		// TODO

		// Simply reset crop state
		ageable.setAge(0);
		block.setBlockData(ageable);

		// Drop items
		for (ItemStack drop : drops) {
			drop_naturally(block, drop);
		}
	}

	public static void drop_naturally(Block block, ItemStack drop) {
		drop_naturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
	}

	public static void drop_naturally(Location loc, ItemStack drop) {
		loc.getWorld().dropItemNaturally(loc, drop);
	}

	public static List<BlockPosition> nearest_blocks_for_radius(int radius) {
		var ret = new ArrayList<BlockPosition>();

		// Use square bounding box
		for (int x = -radius; x <= radius; x++) {
			for (int z = -radius; z <= radius; z++) {
				// Only circular area
				if (x * x + z * z > (radius * radius + 1)) {
					continue;
				}

				ret.add(new BlockPosition(x, 0, z));
			}
		}

		Collections.sort(ret, new BlockPosition.RadiusComparator());
		return ret;
	}

	public static Block next_tillable_block(final Block root_block, int radius, boolean careless) {
		for (var relative_pos : BlockUtil.NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.get(radius - 1)) {
			var block = root_block.getRelative(relative_pos.x, relative_pos.y, relative_pos.z);

			// Check for a tillable material
			if (!is_tillable(block.getType())) {
				continue;
			}

			// Get block above
			var above = block.getRelative(0, 1, 0);
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
		for (var relative_pos : BlockUtil.NEAREST_RELATIVE_BLOCKS_FOR_RADIUS.get(radius - 1)) {
			var block = root_block.getRelative(relative_pos.x, relative_pos.y, relative_pos.z);
			var below = block.getRelative(BlockFace.DOWN);

			// Block below must be farmland and the block itself must be air
			if (below.getType() == farmland_type && block.getType() == Material.AIR) {
				return block;
			}
		}

		// We are outside of the radius.
		return null;
	}
}

