package org.oddlama.vane.enchantments;

import java.util.List;
import java.util.ArrayList;
import org.oddlama.vane.util.Nms;
import org.bukkit.inventory.ItemStack;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;
import net.minecraft.server.v1_16_R1.EnumChatFormat;
import net.minecraft.server.v1_16_R1.ChatMessage;
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
	private NativeEnchantmentWrapper native_wrapper;
	private BukkitEnchantmentWrapper bukkit_wrapper;
	private NamespacedKey key;

	private final ArrayList<Enchantment> supersedes = new ArrayList<>();

	public CustomEnchantment(Context<T> context) {
		super(null);

		// Make namespace
		name = annotation.name();
		context = context.group("enchantment_" + name, "Enable enchantment " + name);
		set_context(context);

		// Create namespaced key
		key = namespaced_key("vane", name);

		// Register and create wrappers
		native_wrapper = new NativeEnchantmentWrapper(this);
		Nms.register_enchantment(get_key(), native_wrapper);

		// After registering in NMS we can create a wrapper for bukkit
		bukkit_wrapper = new BukkitEnchantmentWrapper(this, native_wrapper);
		Enchantment.registerEnchantment(bukkit_wrapper);
	}

	public List<Enchantment> supersedes() {
		return supersedes;
	}

	public void supersedes(Enchantment e) {
		supersedes.add(e);
	}

	public NamespacedKey get_key() {
		return key;
	}

	public String get_name() {
		return name;
	}

	public IChatBaseComponent display_name(int level) {
        var display_name = new ChatMessage(native_wrapper.g());
		display_name.a(EnumChatFormat.RESET, EnumChatFormat.RED);

        if (level != 1 || max_level() != 1) {
            display_name.c(" ").addSibling(new ChatMessage("enchantment.level." + level));
        }

        return display_name;
	}

	public int start_level() {
		return annotation.start_level();
	}

	public int max_level() {
		return annotation.max_level();
	}

	@NotNull
	public EnchantmentTarget item_target() {
		// TODO brrrrrr
		return EnchantmentTarget.BREAKABLE;
	}

	public boolean can_enchant_item(@NotNull ItemStack item) {
		// TODO brrrrrr
		return true;
	}

		// TODO brrrrrr
		// TODO brrrrrr
		// TODO brrrrrr
		// TODO brrrrrr
	public boolean is_treasure() { return false; }
	public boolean is_cursed() { return false; }
	public boolean conflicts_with(@NotNull Enchantment other) { return false; }
}
