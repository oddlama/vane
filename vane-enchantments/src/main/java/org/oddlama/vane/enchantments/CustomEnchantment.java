package org.oddlama.vane.enchantments;

import org.bukkit.inventory.ItemStack;
import static org.oddlama.vane.util.Util.namespaced_key;
import org.bukkit.enchantments.Enchantment;
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

public class CustomEnchantment<T extends Module<T>> extends Listener<T> {
	private VaneEnchantment annotation = getClass().getAnnotation(VaneEnchantment.class);
	private String name;
	private EnchantmentWrapper wrapper;
	private NamespacedKey key;

	public CustomEnchantment(Context<T> context) {
		super(null);

		// Make namespace
		name = annotation.name();
		context = context.group("enchantment_" + name, "Enable enchantment " + name);
		set_context(context);

		// Create namespaced key
		key = namespaced_key("vane", name);

		// Create wrapper
		wrapper = new EnchantmentWrapper(this);
		register();
	}

	public NamespacedKey get_key() {
		return key;
	}

	private void register() {
		//Enchantment.registerEnchantment(wrapper);
		Enchantment.registerEnchantment(wrapper);
	}

	public String get_name() {
		return name;
	}

	public int start_level() {
		return annotation.start_level();
	}

	public int max_level() {
		return annotation.max_level();
	}

	@NotNull
	public EnchantmentTarget item_target() {
		return EnchantmentTarget.BREAKABLE;
	}

	public boolean can_enchant_item(@NotNull ItemStack item) {
		return true;
	}

	public boolean is_treasure() { return false; }
	public boolean is_cursed() { return false; }
	public boolean conflicts_with(@NotNull Enchantment other) { return false; }
}
