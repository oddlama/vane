package org.oddlama.vane.enchantments;

import org.bukkit.inventory.ItemStack;
import static org.oddlama.vane.util.Util.namespaced_key;
import org.bukkit.enchantments.EnchantmentTarget;

import org.oddlama.vane.annotation.VaneModule;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.module.Module;

public class Enchantment extends org.bukkit.enchantments.Enchantment {
	private String name;
	public Enchantment(String name) {
		super(namespaced_key("vane", name));
		this.name = name;
	}

	//@NotNull
	//public org.bukkit.enchantments.Enchantment getEnchantment() {
	//	return Enchantment.getByKey(getKey());
	//}

	@Override
	public int getStartLevel() {
		return 1;
	}

	@Override
	public int getMaxLevel() {
		return 1;
	}

	@NotNull
	@Override
	public EnchantmentTarget getItemTarget() {
		return EnchantmentTarget.BREAKABLE;
	}

	@Override
	public boolean canEnchantItem(@NotNull ItemStack item) {
		return true;
		//return getEnchantment().canEnchantItem(item);
	}

	@SuppressWarnings("deprecation")
	@Deprecated
	@NotNull
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isTreasure() {
		return false;
		//return getEnchantment().isTreasure();
	}

	@SuppressWarnings("deprecation")
	@Deprecated
	@Override
	public boolean isCursed() {
		return false;
		//return getEnchantment().isCursed();
	}

	@Override
	public boolean conflictsWith(@NotNull org.bukkit.enchantments.Enchantment other) {
		return false;
		//return getEnchantment().conflictsWith(other);
	}
}
