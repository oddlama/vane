package org.oddlama.vane.core.item;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.ResourcePackGenerator;
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
	@LangMessage
	public TranslatedMessage lang_name;

	public CustomItemVariant(CustomItem<T, V> parent, U variant) {
		super(null);
		this.parent = parent;
		this.variant = variant;

		if (variant.prefix().equals("")) {
			// Singleton item
			this.variant_name = parent.name();
			set_context(parent.get_context());
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

	/**
	 * Override this and add your related recipes here.
	 */
	public void register_recipes() {}

	/**
	 * Returns the base material. Override if needed.
	 */
	public Material base() {
		return Material.WARPED_FUNGUS_ON_A_STICK;
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
		final var display_name = lang_name.format();
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
	 * Returns an itemstack of this item with the given variant.
	 */
	public <U extends ItemVariantEnum> ItemStack item(U variant) {
		return parent.item(variant, 1);
	}

	/**
	 * Returns an itemstack of this item with the given variant and amount.
	 */
	public <U extends ItemVariantEnum> ItemStack item(U variant, int amount) {
		return parent.item(variant, amount);
	}

	/** Returns the main recipe key */
	public final NamespacedKey recipe_key() { return recipe_key(""); }
	/** Returns a named recipe key */
	public final NamespacedKey recipe_key(String recipe_name) {
		if (recipe_name.equals("")) {
			return namespaced_key("vane", variant_name + "_recipe");
		}
		return namespaced_key("vane", variant_name + "_recipe_" + recipe_name);
	}

	private final void add_recipe_or_throw(NamespacedKey recipe_key, Recipe recipe) {
		if (recipes.containsKey(recipe_key)) {
			throw new RuntimeException("A recipe with the same key ('" + recipe_key + "') is already defined!");
		}
		recipes.put(recipe_key, recipe);
	}

	/**
	 * Adds a related recipe to this item.
	 * Useful if you need non-standard recipes.
	 */
	public final Recipe add_recipe(NamespacedKey recipe_key, Recipe recipe) {
		add_recipe_or_throw(recipe_key, recipe);
		return recipe;
	}

	public final boolean enabled() {
		return variant.enabled();
	}

	/**
	 * Override this to add properties to created item stacks per variant.
	 */
	public ItemStack modify_item_stack(ItemStack item_stack) {
		return item_stack;
	}

	@Override
	public void on_config_change() {
		recipes.keySet().forEach(get_module().getServer()::removeRecipe);
		recipes.clear();

		if (variant().enabled()) {
			register_recipes();
			recipes.values().forEach(get_module().getServer()::addRecipe);
		}
	}

	@Override
	public void on_enable() {
	}

	@Override
	public void on_disable() {
	}

	@Override
	public void on_generate_resource_pack(final ResourcePackGenerator pack) throws IOException {
		final var resource_name = "items/" + variant_name + ".png";
		final var resource = get_module().getResource(resource_name);
		if (resource == null) {
			throw new RuntimeException("Missing resource '" + resource_name + "'. This is a bug.");
		}
		pack.add_item_model(key, resource);
		pack.add_item_override(base().getKey(), key, predicate -> {
			predicate.put("custom_model_data", model_data());
		});
	}
}
