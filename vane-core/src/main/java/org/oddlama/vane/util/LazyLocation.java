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
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

public class LazyLocation {
	private UUID world_id = null;
	private Location location;

	public LazyLocation(final Location location) {
		this.world_id = location.getWorld() == null ? null : location.getWorld().getUID();
		this.location = location;
	}

	public LazyLocation(final UUID world_id, double x, double y, double z, float pitch, float yaw) {
		this.world_id = world_id;
		this.location = new Location(null, x, y, z, pitch, yaw);
	}

	public UUID world_id() { return world_id; }

	public Location location() {
		if (world_id != null && location.getWorld() == null) {
			location.setWorld(Bukkit.getWorld(world_id));
		}

		return location;
	}
}
