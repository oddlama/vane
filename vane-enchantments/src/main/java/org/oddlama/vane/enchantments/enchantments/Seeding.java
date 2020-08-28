package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.enchantments.EnchantmentTarget;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "seeding", max_level = 4, rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.TOOL)
public class Seeding extends CustomEnchantment<Enchantments> {
	public Seeding(Context<Enchantments> context) {
		super(context);
	}
}
