package org.oddlama.vane.enchantments;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_16_R1.ChatMessage;
import net.minecraft.server.v1_16_R1.ChatModifier;
import net.minecraft.server.v1_16_R1.EnumChatFormat;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Nms;

public class CustomEnchantment<T extends Module<T>> extends Listener<T> {
	private VaneEnchantment annotation = getClass().getAnnotation(VaneEnchantment.class);
	private String name;
	private NamespacedKey key;
	private NativeEnchantmentWrapper native_wrapper;
	private BukkitEnchantmentWrapper bukkit_wrapper;

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

	/**
	 * Returns all enchantments that are superseded by this enchantment.
	 */
	public final List<Enchantment> supersedes() {
		return supersedes;
	}

	/**
	 * Adds a superseded enchantment. Superseded enchantments will be removed
	 * from the item when this enchantment is added.
	 */
	public final void supersedes(Enchantment e) {
		supersedes.add(e);
	}

	/**
	 * Returns the namespaced key for this enchantment.
	 */
	public final NamespacedKey get_key() {
		return key;
	}

	/**
	 * Only for internal use.
	 */
	final String get_name() {
		return name;
	}

	/**
	 * Returns the display format for the display name.
	 * By default the same gray color is used as for normal enchantments.
	 */
	public ChatModifier display_format(ChatModifier mod) {
		return mod.setColor(EnumChatFormat.GRAY);
	}

	/**
	 * Determines the display name of the enchantment.
	 * This should most probably not be overridden, as this implementation
	 * already uses clientside translation keys and supports chat formatting.
	 */
	public IChatBaseComponent display_name(int level) {
        var display_name = new ChatMessage(native_wrapper.g());
		display_name.setChatModifier(display_format(
		    display_name.getChatModifier().setItalic(false)));

		if (level != 1 || max_level() != 1) {
			var chat_level = new ChatMessage("enchantment.level." + level);
			chat_level.setChatModifier(display_format(
			    chat_level.getChatModifier().setItalic(false)));
			display_name.c(" ").addSibling(chat_level);
        }

        return display_name;
	}

	/**
	 * The minimum level this enchantment can have. Always fixed to 1.
	 */
	public final int start_level() {
		return 1;
	}

	/**
	 * The maximum level this enchantment can have.
	 * Always reflects the annotation value {@link VaneEnchantment#max_level()}.
	 */
	public final int max_level() {
		return annotation.max_level();
	}

	/**
	 * Determines the minimum enchanting table level at which this enchantment
	 * can occur at the given level.
	 */
	public int min_enchanting_level(int level) {
		return 1 + level * 10;
	}

	/**
	 * Determines the maximum enchanting table level at which this enchantment
	 * can occur at the given level.
	 */
	public int max_enchanting_level(int level) {
		return min_enchanting_level(level) + 5;
	}

	/**
	 * Determines if this enchantment can be obtained with the enchanting table.
	 * Always reflects the annotation value {@link VaneEnchantment#treasure()}.
	 */
	public final boolean is_treasure() {
		return annotation.treasure();
	}

	/**
	 * Determines which item types this enchantment can be applied to.
	 * {@link #can_enchant(ItemStack)} can be used to further limit the applicable items.
	 * Always reflects the annotation value {@link VaneEnchantment#target()}.
	 */
	public final EnchantmentTarget target() {
		return annotation.target();
	}

	/**
	 * Determines the enchantment rarity.
	 * Always reflects the annotation value {@link VaneEnchantment#rarity()}.
	 */
	public final Rarity rarity() {
		return annotation.rarity();
	}

	/**
	 * Determines if this enchantment is compatible with the given enchantment.
	 * By default all enchantments are compatible. Override this if you want
	 * to express conflicting enchantments.
	 */
	public boolean is_compatible(@NotNull Enchantment other) {
		return true;
	}

	/**
	 * Determines if this enchantment can be applied to the given item.
	 * By default this returns true if the {@link #target()} category includes
	 * the given itemstack. Unfortunately this method cannot be used to widen
	 * the allowed items, just to narrow it (limitation due to minecraft server internals).
	 * So for best results, always check super.can_enchant first when overriding.
	 */
	public boolean can_enchant(@NotNull ItemStack item_stack) {
		return annotation.target().includes(item_stack);
	}
}
