package org.oddlama.vane.enchantments.enchantments;

import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;

@VaneEnchantment(name = "rake", max_level = 4)
public class Rake extends CustomEnchantment<Enchantments> {
	public Rake(Context<Enchantments> context) {
		super(context);
	}
}
