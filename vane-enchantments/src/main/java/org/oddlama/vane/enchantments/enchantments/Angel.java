package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@VaneEnchantment(name = "angel", max_level = 5, rarity = Rarity.VERY_RARE, treasure = true)
public class Angel extends CustomEnchantment<Enchantments> {
	@ConfigDouble(def = 0.1, min = 0.0, max = 1.0, desc = "Acceleration percentage. Each tick, the current flying speed is increased X percent towards the target speed. Low values (~0.1) typically result in a smooth acceleration curve and a natural feeling.")
	private double config_acceleration_percentage;
	@ConfigDoubleList(def = {1.0, 1.2, 1.5, 1.9, 2.4}, min = 0.0, desc = "Flying speed in blocks per second for each enchantment level.")
	private List<Double> config_speed;

	public Angel(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return item_stack.getType() == Material.ELYTRA;
	}

	private double get_speed(int level) {
		if (level > 0 && level <= config_speed.size()) {
			return config_speed.get(level - 1);
		}
		return config_speed.get(0);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_move(final PlayerMoveEvent event) {
		// Check sneaking and flying
		final var player = event.getPlayer();
		if (!player.isSneaking() || !player.isGliding()) {
			return;
		}

		// Check enchantment level
		final var chest = player.getEquipment().getChestplate();
		final var level = chest.getEnchantmentLevel(this.bukkit());
		if (level == 0) {
			return;
		}

		var dir = player.getLocation().getDirection();
		if (dir.length() == 0) {
			return;
		}

		// Scale the delta dependent on the angle. Higher angle -> less effect
		var vel = player.getVelocity();
		var delta = config_acceleration_percentage * (1.0 - dir.angle(vel) / Math.PI);
		var factor = get_speed(level);

		// Exponential moving average between velocity and target velocity
		player.setVelocity(vel.multiply(1.0 - delta)
		                      .add(dir.normalize().multiply(delta * factor)));
	}
}
