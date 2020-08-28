package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.enchantments.EnchantmentTarget;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.annotation.enchantment.Rarity;

@VaneEnchantment(name = "hell_bent", rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.ARMOR_HEAD)
public class HellBent extends CustomEnchantment<Enchantments> {
	public HellBent(Context<Enchantments> context) {
		super(context);
	}
}
