package org.oddlama.vane.core.enchantments;

import static org.oddlama.vane.util.Nms.bukkit_enchantment;
import static org.oddlama.vane.util.Nms.enchantment_slot_type;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.util.Nms;

/**
 * Provides a native counterpart for CustomEnchantment.
 * All logic will be forwarded to the CustomEnchantment instance.
 */
public class NativeEnchantmentWrapper extends Enchantment {

	private CustomEnchantment<?> enchantment;

	public NativeEnchantmentWrapper(CustomEnchantment<?> enchantment) {
		super(Enchantment.Rarity.VERY_RARE, enchantment_slot_type(enchantment.target()), new EquipmentSlot[] {});
		this.enchantment = enchantment;
	}

	@Override
	public Enchantment.Rarity getRarity() {
		switch (enchantment.rarity()) {
			case COMMON:
				return Enchantment.Rarity.COMMON;
			case UNCOMMON:
				return Enchantment.Rarity.UNCOMMON;
			case RARE:
				return Enchantment.Rarity.RARE;
			case VERY_RARE:
				return Enchantment.Rarity.VERY_RARE;
			default:
				return Enchantment.Rarity.VERY_RARE;
		}
	}

	public CustomEnchantment<?> custom() {
		return this.enchantment;
	}

	@Override
	public int getMinLevel() {
		return enchantment.min_level();
	}

	@Override
	public int getMaxLevel() {
		return enchantment.max_level();
	}

	@Override
	public int getMinCost(int level) {
		return enchantment.min_cost(level);
	}

	@Override
	public int getMaxCost(int level) {
		return enchantment.max_cost(level);
	}

	@Override
	public boolean isTreasureOnly() {
		return enchantment.is_treasure();
	}

	@Override
	public boolean isDiscoverable() {
		return enchantment.enabled() && enchantment.generate_in_treasure();
	}

	@Override
	public boolean isTradeable() {
		return enchantment.enabled() && enchantment.is_tradeable();
	}

	@Override
	public boolean isCurse() {
		return enchantment.is_curse();
	}

	@Override
	public boolean checkCompatibility(@NotNull Enchantment other) {
		return this != other && enchantment.is_compatible(bukkit_enchantment(other));
	}

	@Override
	public boolean canEnchant(ItemStack itemstack) {
		// Custom item pre-check
		final var bukkit_item = Nms.bukkit_item_stack(itemstack);
		if (!enchantment.allow_custom() && enchantment.get_module().core.item_registry().get(bukkit_item) != null) {
			return false;
		}
		return enchantment.enabled() && enchantment.can_enchant(bukkit_item);
	}
}
