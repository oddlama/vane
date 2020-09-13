package org.oddlama.vane.util;

import static org.oddlama.vane.util.Nms.creative_tab_id;
import static org.oddlama.vane.util.Nms.item_handle;
import static org.oddlama.vane.util.Nms.player_handle;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.Comparator;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

public class LazyBlock {
	private UUID world_id = null;
	private int x;
	private int y;
	private int z;
	private Block block;

	public LazyBlock(final Block block) {
		if (block == null) {
			this.world_id = null;
			this.x = 0;
			this.y = 0;
			this.z = 0;
		} else {
			this.world_id = block.getWorld().getUID();
			this.x = block.getX();
			this.y = block.getY();
			this.z = block.getZ();
		}
		this.block = block;
	}

	public LazyBlock(final UUID world_id, int x, int y, int z) {
		this.world_id = world_id;
		this.x = x;
		this.y = y;
		this.z = z;
		this.block = null;
	}

	public UUID world_id() { return world_id; }
	public int x() { return x; }
	public int y() { return y; }
	public int z() { return z; }

	public Block block() {
		if (world_id != null && block == null) {
			this.block = Bukkit.getWorld(world_id).getBlockAt(x, y, z);
		}

		return block;
	}
}
