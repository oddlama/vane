package org.oddlama.vane.core.item;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.server.v1_16_R1.ChatMessage;
import net.minecraft.server.v1_16_R1.ChatModifier;
import net.minecraft.server.v1_16_R1.EnumChatFormat;
import net.minecraft.server.v1_16_R1.IChatBaseComponent;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.annotation.lang.ResourcePackTranslation;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Nms;

public class CustomItem<T extends Module<T>> extends Listener<T> {
	// Track instances
	private static final Map<Class<?>, CustomItem<?>> instances = new HashMap<>();

	private VaneItem annotation = getClass().getAnnotation(VaneItem.class);
	private String name;
	private NamespacedKey key;

	// Language
	@LangString
	@ResourcePackTranslation(namespace = "vane") // key is set by #lang_name_translation_key()
	public String lang_name;

	public CustomItem(Context<T> context) {
		super(null);

		// Make namespace
		name = annotation.name();
		context = context.group("item_" + name, "Enable item " + name);
		set_context(context);

		// Create namespaced key
		key = namespaced_key("vane", name);

		// Check if instance is already exists
		if (instances.get(getClass()) != null) {
			throw new RuntimeException("Cannot create two instances of a custom item!");
		}
		instances.put(getClass(), this);
	}

	public String lang_name_translation_key() {
		return "vane.item." + name;
	}

	/**
	 * Returns an itemstack for the given custom item, with amount = 1.
	 */
	public static ItemStack item(Class<? extends CustomItem<?>> cls) {
		return item(cls, 1);
	}

	/**
	 * Returns an itemstack for the given custom item with the given amount
	 */
	public static ItemStack item(Class<? extends CustomItem<?>> cls, int amount) {
		final var custom_item = instances.get(cls);
		final var item_stack = new ItemStack(custom_item.base(), amount);
		final var meta = item_stack.getItemMeta();
		//meta.setCustomModelData(custom_item.model_data());
		item_stack.setItemMeta(meta);
		return item_stack;
	}

	/**
	 * Returns the namespaced key for this enchantment.
	 */
	public final NamespacedKey key() {
		return key;
	}

	public final Material base() {
		return annotation.base();
	}
}
