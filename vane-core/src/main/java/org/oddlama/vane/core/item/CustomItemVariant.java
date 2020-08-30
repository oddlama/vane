package org.oddlama.vane.core.item;

import static org.oddlama.vane.util.Util.namespaced_key;

import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.annotation.lang.ResourcePackTranslation;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.ResourcePackGenerator;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class CustomItemVariant<T extends Module<T>, V extends CustomItem<T, V>, U extends ItemVariantEnum> extends ModuleComponent<T> {
	private CustomItem<T, V> parent;

	private NamespacedKey key;
	private String variant_name;
	private U variant;

	// All associated recipes
	private Map<NamespacedKey, Recipe> recipes = new HashMap<>();

	// Language
	@LangString
	@ResourcePackTranslation(namespace = "vane") // key is set by #lang_name_translation_key()
	public String lang_name;

	public CustomItemVariant(CustomItem<T, V> parent, U variant) {
		super(null);
		this.parent = parent;
		this.variant = variant;

		if (variant.prefix().equals("")) {
			// Singleton item
			this.variant_name = parent.name();
		} else {
			// Multi variant item, create sub namespace
			this.variant_name = variant.prefix() + "_" + parent.name();
			set_context(parent.get_context().group(variant.prefix(), "Enable '" + variant.prefix() + "' item variant"));
		}

		// Create namespaced_key
		this.key = namespaced_key("vane", variant_name);

		// Check for duplicate model data
		parent.check_valid_model_data(this);
	}

	public String lang_name_translation_key() {
		return "item.vane." + variant_name;
	}

	/**
	 * Returns the assigned model data.
	 */
	public final int model_data() {
		return parent.model_data(variant);
	}

	/**
	 * Returns the display name for this item variant.
	 */
	public BaseComponent display_name() {
		final var display_name = new TranslatableComponent(lang_name_translation_key());
		display_name.setItalic(false);
		return display_name;
	}

	/**
	 * Returns the namespaced key for this item.
	 */
	public final NamespacedKey key() {
		return key;
	}

	/**
	 * Returns the variant enum for this item.
	 */
	public final U variant() {
		return variant;
	}

	/**
	 * Returns an itemstack of this item variant.
	 */
	public final ItemStack item() {
		return parent.item(this, 1);
	}

	/**
	 * Returns an itemstack of this item variant with the given amount.
	 */
	public final ItemStack item(int amount) {
		return parent.item(this, amount);
	}

	/**
	 * Use this to define recipes for the custom item.
	 * Will automatically be add to the server in on_enable()
	 * and removed in on_disable().
	 */
	public final Recipe add_recipe(NamespacedKey key, Recipe recipe) {
		// TODO get key from recipe... or make overloads... (better i guess)
		recipes.put(key(), recipe);
		return recipe;
	}

	/**
	 * Override this to add properties to created item stacks per variant.
	 */
	public ItemStack modify_item_stack(ItemStack stack) {
		return stack;
	}

	@Override
	public void on_enable() {
		recipes.values().forEach(get_module().getServer()::addRecipe);
	}

	@Override
	public void on_disable() {
		// TODO this good? apparently causes loss of discovered state
		//recipes.keySet().forEach(get_module().getServer()::removeRecipe);
	}

	@Override
	public void on_generate_resource_pack(final ResourcePackGenerator pack) throws IOException {
		pack.add_item_model(key, get_module().getResource("items/" + variant_name + ".png"));
		pack.add_item_override(parent.base().getKey(), key, predicate -> {
			predicate.put("custom_model_data", model_data());
		});
	}
}
