package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.entity.Player;
import static org.oddlama.vane.util.PlayerUtil.apply_elytra_boost;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.Util.ms_to_ticks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.config.ConfigIntList;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import static org.oddlama.vane.util.PlayerUtil.apply_elytra_boost;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.Util.ms_to_ticks;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.oddlama.vane.annotation.config.ConfigDoubleList;
import org.oddlama.vane.annotation.config.ConfigIntList;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import org.oddlama.vane.enchantments.Enchantments;
import net.minecraft.server.v1_16_R1.ChatModifier;

import org.oddlama.vane.enchantments.Enchantments;
import net.minecraft.server.v1_16_R1.ChatModifier;

@VaneEnchantment(name = "take_off", max_level = 4, rarity = Rarity.RARE, treasure = true)
public class TakeOff extends CustomEnchantment<Enchantments> {
	@ConfigDoubleList(def = {0.2, 0.4, 0.6}, min = 0.0, desc = "Boost strength for each enchantment level.")
	private List<Double> config_boost_strengths;

	public TakeOff(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return item_stack.getType() == Material.ELYTRA;
	}

	private double get_boost_strength(int level) {
		if (level > 0 && level <= config_boost_strengths.size()) {
			return config_boost_strengths.get(level - 1);
		}
		return config_boost_strengths.get(0);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_toggle_glide(EntityToggleGlideEvent event) {
		if (!(event.getEntity() instanceof Player) || !event.isGliding()) {
			return;
		}

		// Don't apply for sneaking players
		final var player = (Player)event.getEntity();
		if (player.isSneaking()) {
			return;
		}

		// Check enchantment level
		final var chest = player.getEquipment().getChestplate();
		final var level = chest.getEnchantmentLevel(this.bukkit());
		if (level == 0) {
			return;
		}

		// Apply boost
		apply_elytra_boost(player, get_boost_strength(level));
		damage_item(player, chest, (int)(1.0 + 2.0 * Math.random()));
	}
}
