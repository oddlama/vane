package org.oddlama.vane.core.enchantments;

import net.minecraft.world.item.enchantment.Enchantment;
import org.bukkit.craftbukkit.v1_20_R1.enchantments.CraftEnchantment;
import org.jetbrains.annotations.NotNull;

public class BukkitEnchantmentWrapper extends CraftEnchantment {

	private CustomEnchantment<?> custom_enchantment;

	public BukkitEnchantmentWrapper(CustomEnchantment<?> custom_enchantment, Enchantment native_enchantment) {
		super(native_enchantment);
		this.custom_enchantment = custom_enchantment;
	}

	@Deprecated
	@NotNull
	@Override
	public String getName() {
		return custom_enchantment.get_name();
	}

	public CustomEnchantment<?> custom_enchantment() {
		return custom_enchantment;
	}
}
