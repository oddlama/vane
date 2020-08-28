package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.enchantments.EnchantmentTarget;
import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.enchantments.Enchantments;
import com.destroystokyo.paper.MaterialTags;

@VaneEnchantment(name = "seeding", max_level = 4, rarity = Rarity.COMMON, treasure = true, target = EnchantmentTarget.TOOL)
public class Seeding extends CustomEnchantment<Enchantments> {
	public Seeding(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return MaterialTags.HOES.isTagged(item_stack);
	}
}
