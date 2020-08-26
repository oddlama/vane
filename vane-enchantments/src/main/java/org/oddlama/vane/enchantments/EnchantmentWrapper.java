package org.oddlama.vane.enchantments;

import org.oddlama.vane.util.Nms;
import org.bukkit.inventory.ItemStack;
import static org.oddlama.vane.util.Util.namespaced_key;
import org.bukkit.enchantments.EnchantmentTarget;

import org.oddlama.vane.annotation.VaneModule;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.module.Module;

import net.minecraft.server.v1_16_R1.Enchantment;
import net.minecraft.server.v1_16_R1.EnchantmentSlotType;
import net.minecraft.server.v1_16_R1.EnumItemSlot;

public class EnchantmentWrapper extends Enchantment {
	private CustomEnchantment<?> enchantment;

	public EnchantmentWrapper(CustomEnchantment<?> enchantment) {
		super(Enchantment.Rarity.COMMON, EnchantmentSlotType.BREAKABLE, new EnumItemSlot[] { EnumItemSlot.MAINHAND });
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

	public int a(int i) {
		return 1 + i * 10;
	}

	public int b(int i) {
		return this.a(i) + 5;
	}

	@Override
	public boolean isTreasure() {
		return enchantment.is_treasure();
	}
}
