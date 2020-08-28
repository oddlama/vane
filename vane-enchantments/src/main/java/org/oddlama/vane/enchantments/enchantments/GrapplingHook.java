package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.enchantments.EnchantmentTarget;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "grappling_hook", max_level = 3, rarity = Rarity.UNCOMMON, treasure = true, target = EnchantmentTarget.FISHING_ROD)
public class GrapplingHook extends CustomEnchantment<Enchantments> {
	public GrapplingHook(Context<Enchantments> context) {
		super(context);
	}
}
