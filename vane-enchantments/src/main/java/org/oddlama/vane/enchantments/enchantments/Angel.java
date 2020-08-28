package org.oddlama.vane.enchantments.enchantments;

import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.enchantments.CustomEnchantment;
import org.oddlama.vane.enchantments.Enchantments;
import org.oddlama.vane.annotation.enchantment.Rarity;

@VaneEnchantment(name = "angel", max_level = 5, rarity = Rarity.VERY_RARE, treasure = true)
public class Angel extends CustomEnchantment<Enchantments> {
	public Angel(Context<Enchantments> context) {
		super(context);
	}

	@Override
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return item_stack.getType() == Material.ELYTRA;
	}
}
