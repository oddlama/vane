package org.oddlama.vane.core.item;

import static org.oddlama.vane.util.Util.namespaced_key;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.Collection;

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

	// Track variants
	private final Map<ItemVariantEnum, CustomItemVariant<T, V, ?>> variants = new HashMap<>();

	/**
	 * Single variant item constructor.
	 */
	public CustomItem(Context<T> context) {
		this(context, SingleVariant.values(), CustomItemVariant<T, V, SingleVariant>::new);
	}

	/**
	 * Multi variant item constructor.
	 */
	@SuppressWarnings("unchecked")
	public<U extends ItemVariantEnum> CustomItem(Context<T> context, U[] variant_enum_values, Function2<V, U, CustomItemVariant<T, V, U>> create_instance) {
		super(null);

		// Make namespace
		name = annotation.name();
		context = context.group("item_" + name, "Enable item " + name);
		set_context(context);

		// Check if instance is already exists
		if (instances.get(getClass()) != null) {
			throw new RuntimeException("Cannot create two instances of a custom item!");
		}

		// Create variants
		for (var variant : variant_enum_values) {
			variants.put(variant, create_instance.apply((V)this, variant));
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

	/**
	 * Returns the assigned model data.
	 */
	@SuppressWarnings("unchecked")
	public final int model_data(ItemVariantEnum variant) {
		final var cls = get_module().model_data_enum();

		try {
			final var constant = name.toUpperCase();
			final var custom_item_id = (ModelDataEnum)Enum.valueOf(cls.asSubclass(Enum.class), constant);
			return get_module().model_data(custom_item_id.id(), variant.id());
		} catch (IllegalArgumentException e) {
			get_module().log.log(Level.SEVERE, "Missing enum entry for " + getClass() + ", must be called '" + name.toUpperCase() + "'");
			throw e;
		}
	}

	/**
	 * Returns all variants of this item.
	 */
	public final Collection<CustomItemVariant<T, V, ?>> variants() {
		return variants.values();
	}

	/**
	 * Returns the base material.
	 */
	public final Material base() {
		return annotation.base();
	}

	/**
	 * Returns an itemstack for the given custom item with the given amount
	 */
	public static ItemStack item(Class<?> cls, int amount) {
		//final var custom_item = instances.get(cls);
		//final var item_stack = new ItemStack(custom_item.base(), amount);
		//final var meta = item_stack.getItemMeta();
		//meta.setCustomModelData(custom_item.model_data());
		//meta.setDisplayNameComponent(new BaseComponent[] { custom_item.display_name() });
		//item_stack.setItemMeta(meta);
		//return item_stack;
		return null;
	}

	public static enum SingleVariant implements ItemVariantEnum {
		SINGLETON;

		@Override public int id() { return 0; }
		@Override public String identifier() { return ""; }
		@Override public boolean enabled() { return true; }
	}
}
