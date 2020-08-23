package org.oddlama.vane.util;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

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
}

