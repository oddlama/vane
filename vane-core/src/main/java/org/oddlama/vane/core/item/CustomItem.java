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

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.enchantment.VaneEnchantment;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.annotation.lang.ResourcePackTranslation;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Nms;

public class CustomItem<T extends Module<T>> extends Listener<T> {
	private VaneEnchantment annotation = getClass().getAnnotation(VaneEnchantment.class);
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
	}

	public String lang_name_translation_key() {
		return "vane.item." + name;
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
}
