package org.oddlama.vane.enchantments;

import org.bukkit.inventory.ItemStack;
import static org.oddlama.vane.util.Util.namespaced_key;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;

import org.oddlama.vane.annotation.VaneModule;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.module.Module;

public class EnchantmentWrapper extends Enchantment {
	private CustomEnchantment<?> enchantment;

	public EnchantmentWrapper(CustomEnchantment<?> enchantment) {
		super(enchantment.get_key());
		this.enchantment = enchantment;
	}

	@Override
	public int getStartLevel() {
		return enchantment.start_level();
	}

	@Override
	public int getMaxLevel() {
		return enchantment.max_level();
	}

	@NotNull
	@Override
	public EnchantmentTarget getItemTarget() {
		return enchantment.item_target();
	}

	@Override
	public boolean canEnchantItem(@NotNull ItemStack item) {
		return enchantment.can_enchant_item(item);
	}

	@SuppressWarnings("deprecation")
	@Deprecated
	@NotNull
	@Override
	public String getName() {
		return enchantment.get_name();
	}

	@Override
	public boolean isTreasure() {
		return enchantment.is_treasure();
	}

	@SuppressWarnings("deprecation")
	@Deprecated
	@Override
	public boolean isCursed() {
		return enchantment.is_cursed();
	}

	@Override
	public boolean conflictsWith(@NotNull Enchantment other) {
		return enchantment.conflicts_with(other);
	}
}
