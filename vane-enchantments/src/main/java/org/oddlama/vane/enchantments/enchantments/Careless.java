package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.enchantments.EnchantmentTarget;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "careless", rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.TOOL)
public class Careless extends CustomEnchantment<Enchantments> {
	public Careless(Context<Enchantments> context) {
		super(context);
	}
}
