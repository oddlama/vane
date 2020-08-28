package org.oddlama.vane.util;

import org.bukkit.SoundCategory;
import org.bukkit.Sound;
import static org.oddlama.vane.util.ItemUtil.damage_item;
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

	public static void till_block(final Player player, final Block block) {
		// Till block
		block.setType(Material.FARMLAND);

		// Play sound, damage item and swing arm
		player.getWorld().playSound(player.getLocation(), Sound.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
		player.swingMainHand();
	}
}

