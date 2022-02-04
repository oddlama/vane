package org.oddlama.vane.core.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;

/**
 *
 * Represents a custom item. A custom item can have different variants (e.g. stone, iron, golden, ...)
 * Remember that you should never reuse id's previously in use. Use the disabled tag for this to prevent
 * recipes from registering and events from being processed.
 *
 * @param <ModuleT> Module Type
 * @param <ItemT> Item Self Type
 */
public class CustomItem<ModuleT extends Module<ModuleT>, ItemT extends CustomItem<ModuleT, ItemT>>
	extends Listener<ModuleT>
	implements Model<CustomItem<ModuleT, ItemT>>, Keyed {

	private static final LegacyModelRegistry registry = new LegacyModelRegistry();
	private final VaneItem annotation = getClass().getAnnotation(VaneItem.class);
	private final String name;
	private final Class<? extends ItemVariantEnum> variant_enum_class;
	private final ItemVariantEnum variant_min;
	private final ItemVariantEnum variant_max;

	// Track variants
	private final List<CustomItemVariant<ModuleT, ItemT, ?>> variants = new ArrayList<>();
	private NamespacedKey key;

	/**
	 * Single variant item constructor.
	 */
	public CustomItem(
		Context<ModuleT> context,
		Function2<ItemT, SingleVariant, CustomItemVariant<ModuleT, ItemT, SingleVariant>> create_instance
	) {
		this(context, SingleVariant.class, SingleVariant.values(), create_instance);
	}

	/**
	 * Multi variant item constructor.
	 */
	@SuppressWarnings("unchecked")
	public <U extends ItemVariantEnum> CustomItem(
		Context<ModuleT> context,
		Class<U> variant_enum_class,
		U[] variant_enum_values,
		Function2<ItemT, U, CustomItemVariant<ModuleT, ItemT, U>> create_instance
	) {
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
		if (registry.is_registered(getClass())) {
			throw new RuntimeException("Cannot create two instances of a custom item!");
		}

		// Create variants
		for (var variant : variant_enum_values) {
			final var v = create_instance.apply((ItemT) this, variant);
			variants.add(v);
			registry.registerVariant(this, v, variant);
		}

		registry.register(getClass(), this);
		instances().put(getClass(), this);
	}

	abstract Map<Class<?>, CustomItem<ModuleT, ItemT>> instances();

	/**
	 * Asserts that there is no other item with the same model data
	 */
	protected final void check_valid_model_data(CustomItemVariant<ModuleT, ItemT, ?> variant) {
		registry.check_valid_model_data(this, variant);
	}

	public final String name() {
		return name;
	}

	final void assert_correct_variant_class(ItemVariantEnum variant) {
		if (!variant.getClass().equals(variant_enum_class)) {
			throw new RuntimeException(
				"Invalid ItemVariantEnum class " +
				variant.getClass() +
				" for item " +
				getClass() +
				": expected " +
				variant_enum_class
			);
		}
	}

	public final boolean has_netherite_conversion() {
		return netherite_conversion_from() != null && netherite_conversion_to() != null;
	}

	public ItemVariantEnum netherite_conversion_from() {
		return null;
	}

	public ItemVariantEnum netherite_conversion_to() {
		return null;
	}

	/**
	 * Returns the assigned model data.
	 * Deprecated, to be migrated to the ModelRegistry
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	public final int model_data(ItemVariantEnum variant) {
		assert_correct_variant_class(variant);
		final var cls = get_module().model_data_enum();
		try {
			final var constant = name.toUpperCase();
			final var custom_item_id = (ModelDataEnum) Enum.valueOf(cls.asSubclass(Enum.class), constant);
			return get_module().model_data(custom_item_id.id(), variant.ordinal());
		} catch (IllegalArgumentException e) {
			get_module()
				.log.log(
					Level.SEVERE,
					"Missing enum entry for " + getClass() + ", must be called '" + name.toUpperCase() + "'"
				);
			throw e;
		}
	}

	/**
	 * Returns all variants of this item.
	 */
	public final List<CustomItemVariant<ModuleT, ItemT, ?>> variants() {
		return variants;
	}

	/**
	 * Returns the lower bound for this custom item.
	 */
	@Deprecated
	private int model_data_range_lower_bound() {
		return model_data(variant_min);
	}

	/**
	 * Returns the upper bound for this custom item.
	 */
	@Deprecated
	private int model_data_range_upper_bound() {
		return model_data(variant_max);
	}

	public static boolean has_custom_model(@NotNull ItemStack item) {
		return item.hasItemMeta() && item.getItemMeta().hasCustomModelData();
	}

	public static CustomItemVariant from_item(@NotNull ItemStack item) {
		final var meta = item.getItemMeta();
		if (!meta.hasCustomModelData()) {
			return null;
		}

		return registry.getVariant(meta.getCustomModelData(), item);
	}

	/**
	 * Returns the variant of the given item or null if the item
	 * is not an instance of this custom item.
	 */
	@SuppressWarnings("unchecked")
	public <U> U variant_of(@NotNull ItemStack item) {
		final var meta = item.getItemMeta();
		if (meta == null) {
			return null;
		}
		if (!meta.hasCustomModelData()) {
			return null;
		}

		// Check custom model data range
		final var custom_model_data = meta.getCustomModelData();
		if (
			model_data_range_lower_bound() <= custom_model_data && custom_model_data <= model_data_range_upper_bound()
		) {
			return (U) variants().get(custom_model_data - model_data_range_lower_bound());
		}

		return null;
	}

	/**
	 * Returns an itemstack of this item with the given variant.
	 */
	public <U extends ItemVariantEnum> ItemStack item(U variant) {
		return item(variant, 1);
	}

	/**
	 * Returns an itemstack of this item with the given variant and amount.
	 */
	public <U extends ItemVariantEnum> ItemStack item(U variant, int amount) {
		return item(getClass(), variant, amount);
	}

	/**
	 * Returns an itemstack of this item with the given variant.
	 */
	public <U extends ItemVariantEnum> ItemStack item(CustomItemVariant<ModuleT, ItemT, U> variant) {
		return item(variant, 1);
	}

	/**
	 * Returns an itemstack of this item with the given variant and amount.
	 */
	public <U extends ItemVariantEnum> ItemStack item(CustomItemVariant<ModuleT, ItemT, U> variant, int amount) {
		assert_correct_variant_class(variant.variant());
		return construct_item_stack(amount, this, variant);
	}

	/**
	 * Returns the variant for the given registered item and variant enum.
	 */
	@SuppressWarnings("unchecked")
	public <U> U variant_of(Class<?> cls, ItemVariantEnum variant) {
		final var custom_item = instances().get(cls);
		custom_item.assert_correct_variant_class(variant);
		return (U) custom_item.variants().get(variant.ordinal());
	}

	/**
	 * Returns an itemstack for the given custom item with the given amount
	 */
	public <U extends ItemVariantEnum> ItemStack item(Class<?> cls, U variant, int amount) {
		final var custom_item = instances().get(cls);
		custom_item.assert_correct_variant_class(variant);
		final var custom_item_variant = custom_item.variants().get(variant.ordinal());
		return construct_item_stack(amount, custom_item, custom_item_variant);
	}

	protected <B extends CustomItemVariant<?, ?, ?>> ItemStack prepare(@NotNull ItemStack item_stack, B variant) {
		final var meta = item_stack.getItemMeta();
		meta.setCustomModelData(this.model_data(variant.variant()));
		// TODO: This is nuking players custom names probably
		meta.displayName(variant.display_name());
		item_stack.setItemMeta(meta);
		return this.modify_item_stack(variant.modify_item_stack(item_stack));
	}

	private static <A extends CustomItem<?, ?>, B extends CustomItemVariant<?, ?, ?>> ItemStack construct_item_stack(
		int amount,
		A custom_item,
		B custom_item_variant
	) {
		final var item_stack = new ItemStack(custom_item_variant.base(), amount);
		return custom_item.prepare(item_stack, custom_item_variant);
	}

	public <U extends ItemVariantEnum> ItemStack modify_variant(@NotNull ItemStack item, U variant) {
		final var item_variant = from_item(item);
		if (item_variant == null) throw new IllegalArgumentException("item must be a valid variant");
		item_variant.<U>assert_correct_variant_class(variant);
		final var custom_item_variant = item_variant.as(variant);
		final var item_stack = item.clone();
		return custom_item_variant.prepare(item_stack);
	}

	/**
	 * Convert an existing item to a custom item. Base type will be changed,
	 * but e.g. Enchantments and attributes will be kept.
	 */
	public static <A extends CustomItem<?, ?>, U extends ItemVariantEnum> ItemStack convert_existing(
		@NotNull ItemStack item,
		A custom_item,
		U variant
	) {
		custom_item.assert_correct_variant_class(variant);
		final var item_stack = item.clone();
		final var custom_item_variant = custom_item.variants().get(variant.ordinal());
		item_stack.setType(custom_item_variant.base());
		return custom_item.prepare(item_stack, custom_item_variant);
	}

	/**
	 * Override this to add properties to created item stacks.
	 * Will be called after CustomItemVariant.modify_item_stack.
	 */
	public ItemStack modify_item_stack(ItemStack item_stack) {
		return item_stack;
	}

	@Override
	public ItemStack item() {
		return variants.get(0).item();
	}

	@Override
	public int custom_model_data() {
		return variants.get(0).custom_model_data();
	}

	public static enum SingleVariant implements ItemVariantEnum {
		SINGLETON;

		@Override
		public String prefix() {
			return "";
		}

		@Override
		public boolean enabled() {
			return true;
		}
	}

	@Override
	public @NotNull NamespacedKey getKey() {
		return key;
	}
}
