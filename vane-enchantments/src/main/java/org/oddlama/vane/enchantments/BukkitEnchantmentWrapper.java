package org.oddlama.vane.enchantments;

import org.oddlama.vane.util.Nms;
import org.bukkit.inventory.ItemStack;
import static org.oddlama.vane.util.Util.namespaced_key;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.NamespacedKey;

import org.oddlama.vane.annotation.VaneModule;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.command.params.AnyParam;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Module;

import net.minecraft.server.v1_16_R1.Enchantment;
import org.bukkit.craftbukkit.v1_16_R1.enchantments.CraftEnchantment;

public class BukkitEnchantmentWrapper extends CraftEnchantment {
	private CustomEnchantment<?> custom_enchantment;

	public BukkitEnchantmentWrapper(CustomEnchantment<?> custom_enchantment, Enchantment native_enchantment) {
		super(native_enchantment);
		this.custom_enchantment = custom_enchantment;
	}

	@SuppressWarnings("deprecation")
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
