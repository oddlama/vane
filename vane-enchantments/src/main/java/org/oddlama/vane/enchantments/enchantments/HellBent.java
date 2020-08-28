package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "hell_bent", rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.ARMOR_HEAD)
public class HellBent extends CustomEnchantment<Enchantments> {
	public HellBent(Context<Enchantments> context) {
		super(context);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_damage(final EntityDamageEvent event) {
		final var entity = event.getEntity();
		if (!(entity instanceof Player) || event.getCause() != EntityDamageEvent.DamageCause.FLY_INTO_WALL) {
			return;
		}

		// Get helmet
		final var player = (Player)entity;
		final var helmet = player.getEquipment().getHelmet();
		if (helmet == null) {
			return;
		}

		// Check enchantment
		if (helmet.getEnchantmentLevel(this.bukkit()) == 0) {
			return;
		}

		event.setCancelled(true);
	}
}
