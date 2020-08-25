package org.oddlama.vane.util;

import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.Location;

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
}

