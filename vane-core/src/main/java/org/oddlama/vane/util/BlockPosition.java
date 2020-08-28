package org.oddlama.vane.util;

import org.bukkit.block.Block;
import java.util.Comparator;

public class BlockPosition {
	public int x;
	public int y;
	public int z;

	public BlockPosition() {
		this.x = 0;
		this.y = 0;
		this.z = 0;
	}

	public BlockPosition(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (!(o instanceof BlockPosition))
			return false;

		var p = (BlockPosition)o;
		return x == p.x && y == p.y && z == p.z;
	}

	@Override
	public int hashCode() {
		int result = x;
		result = 31 * result + y;
		result = 31 * result + z;
		return result;
	}

	public Block relative(Block origin) {
		return origin.getRelative(x, y, z);
	}

	public static class RadiusComparator implements Comparator<BlockPosition> {
		@Override
		public int compare(BlockPosition a, BlockPosition b) {
			return (a.x * a.x + a.y * a.y + a.z * a.z) - (b.x * b.x + b.y * b.y + b.z * b.z);
		}
	}
}
