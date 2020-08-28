package org.oddlama.vane.enchantments.enchantments;

import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "unbreakable", rarity = Rarity.RARE, treasure = true)
public class Unbreakable extends CustomEnchantment<Enchantments> {
	public Unbreakable(Context<Enchantments> context) {
		super(context);
	}
}
