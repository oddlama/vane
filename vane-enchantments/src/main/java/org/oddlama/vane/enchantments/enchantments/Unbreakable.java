package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemDamageEvent;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "unbreakable", rarity = Rarity.RARE, treasure = true)
public class Unbreakable extends CustomEnchantment<Enchantments> {
	public Unbreakable(Context<Enchantments> context) {
		super(context);

		supersedes(Enchantment.DURABILITY);
		supersedes(Enchantment.MENDING);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_item_damage(final PlayerItemDamageEvent event) {
		// Check enchantment
		final var item = event.getItem();
		if (item.getEnchantmentLevel(this.bukkit()) == 0) {
			return;
		}

		// Prevent damage
		event.setDamage(0);
		event.setCancelled(true);

		// Set item unbreakable to prevent further event calls
		final var meta = item.getItemMeta();
		meta.setUnbreakable(true);
		item.setItemMeta(meta);
	}
}
