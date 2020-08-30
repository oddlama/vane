package org.oddlama.vane.core.item;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.io.IOException;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;

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
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.ResourcePackGenerator;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;

/**
 * Represents a custom item. A custom item can have different variants (e.g. stone, iron, golden, ...)
 * Remember that you should never reuse id's previously in use. Use the disabled tag for this to prevent
 * recipes from registering and events from being processed.
 */
public class CustomItem<T extends Module<T>, V extends CustomItem<T, V>> extends Listener<T> {
	// Track instances
	private static final Map<Class<?>, CustomItem<?, ?>> instances = new HashMap<>();

	private VaneItem annotation = getClass().getAnnotation(VaneItem.class);
	private String name;
	private Class<? extends ItemVariantEnum> variant_enum_class;
	private ItemVariantEnum variant_min;
	private ItemVariantEnum variant_max;

	// Track variants
	private final List<CustomItemVariant<T, V, ?>> variants = new ArrayList<>();

	/**
	 * Single variant item constructor.
	 */
	public CustomItem(Context<T> context) {
		this(context, SingleVariant.class, SingleVariant.values(), CustomItemVariant<T, V, SingleVariant>::new);
	}

	/**
	 * Multi variant item constructor.
	 */
	@SuppressWarnings("unchecked")
	public<U extends ItemVariantEnum> CustomItem(Context<T> context, Class<U> variant_enum_class, U[] variant_enum_values, Function2<V, U, CustomItemVariant<T, V, U>> create_instance) {
		super(null);
		this.variant_enum_class = variant_enum_class;

		// Make namespace
		name = annotation.name();
		context = context.group("item_" + name, "Enable item " + name);
		set_context(context);

		// Track lowest and highest variant
		variant_min = variant_enum_values[0];
		variant_max = variant_enum_values[variant_enum_values.length - 1];

		// Check if instance is already exists
		if (instances.get(getClass()) != null) {
			throw new RuntimeException("Cannot create two instances of a custom item!");
		}

		// Create variants
		for (var variant : variant_enum_values) {
			variants.add(create_instance.apply((V)this, variant));
		}

		instances.put(getClass(), this);
	}

	/**
	 * Asserts that there is no other item with the same model data
	 */
	protected final void check_valid_model_data(CustomItemVariant<T, V, ?> variant) {
		for (var item : instances.values()) {
			if (item.base() == base()) {
				for (var other_variant : item.variants()) {
					if (other_variant.model_data() == variant.model_data()) {
						throw new RuntimeException("Cannot register custom item " + getClass() + " variant " + variant
								+ " with the same base material and model_data as " + item.getClass() + " variant " + other_variant);
					}
				}
			}
		}
	}

	public final String name() {
		return name;
	}

	private final void assert_correct_variant_class(ItemVariantEnum variant) {
		if (!variant.getClass().equals(variant_enum_class)) {
			throw new RuntimeException("Invalid ItemVariantEnum class " + variant.getClass() + " for item " + getClass() + ": expected " + variant_enum_class);
		}
	}

	/**
	 * Returns the assigned model data.
	 */
	@SuppressWarnings("unchecked")
	public final int model_data(ItemVariantEnum variant) {
		assert_correct_variant_class(variant);
		final var cls = get_module().model_data_enum();
		try {
			final var constant = name.toUpperCase();
			final var custom_item_id = (ModelDataEnum)Enum.valueOf(cls.asSubclass(Enum.class), constant);
			return get_module().model_data(custom_item_id.id(), variant.ordinal());
		} catch (IllegalArgumentException e) {
			get_module().log.log(Level.SEVERE, "Missing enum entry for " + getClass() + ", must be called '" + name.toUpperCase() + "'");
			throw e;
		}
	}

	/**
	 * Returns all variants of this item.
	 */
	public final List<CustomItemVariant<T, V, ?>> variants() {
		return variants;
	}

	/**
	 * Returns the base material.
	 */
	public final Material base() {
		return annotation.base();
	}

	/**
	 * Returns the lower bound for this custom item.
	 */
	private int model_data_range_lower_bound() {
		return model_data(variant_min);
	}

	/**
	 * Returns the upper bound for this custom item.
	 */
	private int model_data_range_upper_bound() {
		return model_data(variant_max);
	}

	/**
	 * Returns the variant of the given item or null if the item
	 * is not an instance of this custom item. Also returns null
	 * the corresponding item variant is disabled.
	 */
	@SuppressWarnings("unchecked")
	public<U> U variant_of(@NotNull ItemStack item) {
		// Check base item
		if (item.getType() != base()) {
			return null;
		}

		// Check custom model data range
		final var meta = item.getItemMeta();
		final var custom_model_data = meta.getCustomModelData();
		if (model_data_range_lower_bound() <= custom_model_data && custom_model_data <= model_data_range_upper_bound()) {
			return (U)variants().get(custom_model_data - model_data_range_lower_bound());
		}

		return null;
	}

	/**
	 * Returns an itemstack of this item with the given variant.
	 */
	public <U extends ItemVariantEnum> ItemStack item(CustomItemVariant<T, V, U> variant) {
		return item(variant, 1);
	}

	private static ItemStack construct_item_stack(Material base, int amount, int model_data, BaseComponent display_name) {
		final var item_stack = new ItemStack(base, amount);
		final var meta = item_stack.getItemMeta();
		meta.setCustomModelData(model_data);
		meta.setDisplayNameComponent(new BaseComponent[] { display_name });
		item_stack.setItemMeta(meta);
		return item_stack;
	}

	/**
	 * Returns an itemstack of this item with the given variant and amount.
	 */
	public <U extends ItemVariantEnum> ItemStack item(CustomItemVariant<T, V, U> variant, int amount) {
		assert_correct_variant_class(variant.variant());
		return construct_item_stack(base(), amount, model_data(variant.variant()), variant.display_name());
	}

	/**
	 * Returns an itemstack for the given custom item with the given amount
	 */
	public static <U extends ItemVariantEnum> ItemStack item(Class<?> cls, U variant, int amount) {
		final var custom_item = instances.get(cls);
		custom_item.assert_correct_variant_class(variant);
		final var custom_item_variant = custom_item.variants().get(variant.ordinal());
		return construct_item_stack(custom_item.base(), amount, custom_item.model_data(variant), custom_item_variant.display_name());
	}

	public static enum SingleVariant implements ItemVariantEnum {
		SINGLETON;

		@Override public String prefix() { return ""; }
		@Override public boolean enabled() { return true; }
	}
}
