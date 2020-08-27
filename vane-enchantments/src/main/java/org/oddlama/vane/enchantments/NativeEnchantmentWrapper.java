package org.oddlama.vane.enchantments;

import org.oddlama.vane.util.Nms;
import static org.oddlama.vane.util.Util.namespaced_key;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;
import static org.oddlama.vane.util.Nms.bukkit_enchantment;
import static org.oddlama.vane.util.Nms.enchantment_slot_type;
import org.bukkit.enchantments.EnchantmentTarget;

import net.minecraft.server.v1_16_R1.ItemStack;
import org.oddlama.vane.annotation.VaneModule;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.core.module.Module;

import net.minecraft.server.v1_16_R1.Enchantment;
import net.minecraft.server.v1_16_R1.EnumItemSlot;

/**
 * Provides a native counterpart for CustomEnchantment.
 * All logic will be forwarded to the CustomEnchantment instance.
 */
public class NativeEnchantmentWrapper extends Enchantment {
	private CustomEnchantment<?> enchantment;

	public NativeEnchantmentWrapper(CustomEnchantment<?> enchantment) {
		super(Enchantment.Rarity.VERY_RARE, enchantment_slot_type(enchantment.target()), new EnumItemSlot[] { });
		this.enchantment = enchantment;
	}

	// rarity()
	@Override
    public Enchantment.Rarity d() {
		switch (enchantment.rarity()) {
			case COMMON:    return Enchantment.Rarity.COMMON;
			case UNCOMMON:  return Enchantment.Rarity.UNCOMMON;
			case RARE:      return Enchantment.Rarity.RARE;
			case VERY_RARE: return Enchantment.Rarity.VERY_RARE;
			default:        return Enchantment.Rarity.VERY_RARE;
		}
    }

	@Override
	public int getStartLevel() {
		return enchantment.start_level();
	}

	@Override
	public int getMaxLevel() {
		return enchantment.max_level();
	}

	// min_enchanting_level()
	@Override
	public int a(int level) {
		return enchantment.min_enchanting_level(level);
	}

	// max_enchanting_level()
	@Override
	public int b(int level) {
		return enchantment.max_enchanting_level(level);
	}

	// display_name()
	@Override
	public IChatBaseComponent d(int level) {
		return enchantment.display_name(level);
	}

	@Override
	public boolean isTreasure() {
		return enchantment.is_treasure();
	}

	// is_compatible()
	@Override
	public boolean a(@NotNull Enchantment other) {
		return this != other && enchantment.is_compatible(bukkit_enchantment(other));
	}

	@Override
	public boolean canEnchant(ItemStack itemstack) {
		return enchantment.can_enchant(Nms.bukkit_item_stack(itemstack));
	}
}
