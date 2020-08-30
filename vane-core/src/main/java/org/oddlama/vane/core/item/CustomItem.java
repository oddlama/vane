package org.oddlama.vane.core.item;

import static org.oddlama.vane.util.Util.namespaced_key;

import org.bukkit.inventory.Recipe;
import java.io.IOException;
import org.oddlama.vane.core.ResourcePackGenerator;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;

import net.minecraft.server.v1_16_R2.ChatMessage;
import net.minecraft.server.v1_16_R2.ChatModifier;
import net.minecraft.server.v1_16_R2.EnumChatFormat;
import net.minecraft.server.v1_16_R2.IChatBaseComponent;

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

	// All associated recipes
	private Map<NamespacedKey, Recipe> recipes = new HashMap<>();

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

		// Check for duplicate model data
		for (var item : instances.values()) {
			if (item.base() == base() && item.model_data() == model_data()) {
				throw new RuntimeException("Cannot register " + getClass() + " with the same base material and model_data as " + item.getClass());
			}
		}

		instances.put(getClass(), this);
	}

	public String lang_name_translation_key() {
		return "item.vane." + name;
	}

	/**
	 * Returns the assigned model data.
	 */
	@SuppressWarnings("unchecked")
	public final int model_data() {
		final var cls = get_module().model_data_enum();

		try {
			final var constant = name.toUpperCase();
			final var value = (ModelDataEnum)Enum.valueOf(cls.asSubclass(Enum.class), constant);
			return get_module().model_data(value.id());
		} catch (IllegalArgumentException e) {
			get_module().log.log(Level.SEVERE, "Missing enum entry for " + getClass() + ", must be called '" + name.toUpperCase() + "'");
			throw e;
		}
	}

	/**
	 * Returns the base material.
	 */
	public final Material base() {
		return annotation.base();
	}

	/**
	 * Returns the base material.
	 */
	public BaseComponent display_name() {
		final var display_name = new TranslatableComponent(lang_name_translation_key());
		display_name.setItalic(false);
		return display_name;
	}

	/**
	 * Returns an itemstack for this custom item, with amount = 1.
	 */
	public ItemStack item() {
		return item(1);
	}

	/**
	 * Returns an itemstack for this custom item, with amount = 1.
	 */
	public ItemStack item(int amount) {
		return item(getClass(), amount);
	}

	/**
	 * Returns an itemstack for the given custom item, with amount = 1.
	 */
	public static ItemStack item(Class<?> cls) {
		return item(cls, 1);
	}

	/**
	 * Returns an itemstack for the given custom item with the given amount
	 */
	public static ItemStack item(Class<?> cls, int amount) {
		final var custom_item = instances.get(cls);
		final var item_stack = new ItemStack(custom_item.base(), amount);
		final var meta = item_stack.getItemMeta();
		meta.setCustomModelData(custom_item.model_data());
		meta.setDisplayNameComponent(new BaseComponent[] { custom_item.display_name() });
		item_stack.setItemMeta(meta);
		return item_stack;
	}

	/**
	 * Returns the namespaced key for this enchantment.
	 */
	public final NamespacedKey key() {
		return key;
	}

	/**
	 * Returns a namespaced key for the main related recipe.
	 */
	public final NamespacedKey recipe_key() {
		return namespaced_key("vane", name + "_recipe");
	}

	/**
	 * Returns a namespaced key for a related recipe.
	 */
	public final NamespacedKey recipe_key(String identifier) {
		return namespaced_key("vane", name + "_recipe_" + identifier);
	}

	/**
	 * Use this to define recipes for the custom item.
	 * Will automatically be add to the server in on_enable()
	 * and removed in on_disable().
	 */
	public final Recipe add_recipe(NamespacedKey key, Recipe recipe) {
		// TODO get key from recipe... or make overloads... (better i guess)
		recipes.put(key, recipe);
		return recipe;
	}

	@Override
	public void on_enable() {
		recipes.values().forEach(get_module().getServer()::addRecipe);
	}

	@Override
	public void on_disable() {
		// TODO this good? apparently causes loss of discovered state
		recipes.keySet().forEach(get_module().getServer()::removeRecipe);
	}

	@Override
	public void on_generate_resource_pack(final ResourcePackGenerator pack) throws IOException {
		pack.add_item_model(key, get_module().getResource("items/" + name + ".png"));
		pack.add_item_override(base().getKey(), key, predicate -> {
			predicate.put("custom_model_data", model_data());
		});
	}
}
