package org.oddlama.vane.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

public class PlayerUtil {
	public static void apply_elytra_boost(final Player player, double factor) {
		final var v = player.getLocation().getDirection();
		v.normalize();
		v.multiply(factor);

		// Set velocity, play sound
		player.setVelocity(player.getVelocity().add(v));
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.4f, 2.0f);
	}
}

