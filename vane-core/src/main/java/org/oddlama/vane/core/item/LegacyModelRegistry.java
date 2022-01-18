package org.oddlama.vane.core.item;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.module.Module;

public class LegacyModelRegistry {

	/**
	 * The base offset for any model data used by vane plugins.
	 */
	// "vane" = 0x76616e65 in ascii, but the value will be saved as float (json...), so only -2^24 - 2^24 can accurately be represented.
	// therefore, we use 0x76616e as the base value.
	// 1.18.1: You would think this is just a magic value at this point, due to the move to CustomModelData.
	//         You would be wrong.
	//         https://minecraft.fandom.com/wiki/Model
	//         "custom_model_data": Used on any item and is compared to the tag.CustomModelData NBT, expressed in an integer value. The number
	//         is still internally converted to float, causing a precision loss for some numbers above 16 million. If the value read from the
	//         item data is greater than or equal to the value used for the predicate, the predicate is positive.
	public static final int ITEM_DATA_BASE_OFFSET = 0x76616e;
	/**
	 * The amount of reserved model data id's per section (usually one section per plugin).
	 */
	public static final int ITEM_DATA_SECTION_SIZE = 0x10000; // 0x10000 = 65k/** The amount of reserved model data id's per section (usually one section per plugin). */
	public static final int ITEM_VARIANT_SECTION_SIZE = (1 << 6); // 65k total → 1024 (items) * 64 (variants per item)/** Returns the item model data given the section and id */

	public enum Section {
		Trifles(0),
		Enchantments(1),
		Icons(9);

		private final int id;

		Section(int i) {
			this.id = i;
		}
	}

	public static int calculate_model_key(Section section, int item_id, int variant_id) {
		return (
			ITEM_DATA_BASE_OFFSET +
			section.id *
			ITEM_DATA_SECTION_SIZE +
			item_id *
			ITEM_VARIANT_SECTION_SIZE +
			variant_id
		);
	}

	// Track instances
	private static final Map<Class<?>, CustomItem<?, ?>> instances = new HashMap<>();
	// Reverse lookup model data -> custom item class, variant
	static final Map<Integer, ReverseLookupEntry> reverse_lookup = new HashMap<>();

	public <V extends CustomItem<T, V>, T extends Module<T>> void register(
		Class<? extends CustomItem> clazz,
		CustomItem item
	) {
		instances.put(clazz, item);
	}

	public boolean is_registered(Class<? extends CustomItem> clazz) {
		return instances.get(clazz) != null;
	}

	public <V extends CustomItem<T, V>, T extends Module<T>, U extends ItemVariantEnum> void registerVariant(
		CustomItem item,
		CustomItemVariant<T, V, U> v,
		U variant
	) {
		LegacyModelRegistry.reverse_lookup.put(v.model_data(), new ReverseLookupEntry(item, variant));
	}

	/**
	 * Asserts that there is no other item with the same model data
	 */
	protected final <T extends Module<T>, V extends CustomItem<T, V>> void check_valid_model_data(
		CustomItem<T, V> parent,
		CustomItemVariant<T, V, ?> variant
	) {
		for (var registeredItem : instances.values()) {
			for (var other_variant : registeredItem.variants()) {
				if (other_variant.base() == variant.base()) {
					if (other_variant.model_data() == variant.model_data()) {
						throw new RuntimeException(
							"Cannot register custom item " +
							parent.getClass() +
							" variant " +
							variant +
							" with the same base material " +
							variant.base() +
							" and model_data as " +
							registeredItem.getClass() +
							" variant " +
							other_variant
						);
					}
				}
			}
		}
	}

	/**
	 * Returns the assigned model data.
	 */
	@SuppressWarnings("unchecked")
	public final int model_data(CustomItem item, ItemVariantEnum variant) {
		item.assert_correct_variant_class(variant);
		final var cls = item.get_module().model_data_enum();
		try {
			final var constant = item.name().toUpperCase();
			final var custom_item_id = (ModelDataEnum) Enum.valueOf(cls.asSubclass(Enum.class), constant);
			return item.get_module().model_data(custom_item_id.id(), variant.ordinal());
		} catch (IllegalArgumentException e) {
			item
				.get_module()
				.log.log(
					Level.SEVERE,
					"Missing enum entry for " + getClass() + ", must be called '" + item.name().toUpperCase() + "'"
				);
			throw e;
		}
	}

	public <V extends CustomItem<?, V>> CustomItem<?, ?> getItem(int customModelData) {
		final LegacyModelRegistry.ReverseLookupEntry entry = reverse_lookup.get(customModelData);
		if (entry == null) return null;
		return entry.custom_item;
	}

	public <V extends CustomItem<?, V>> CustomItemVariant<?, V, ?> getVariant(int customModelData, ItemStack stack) {
		final CustomItem<?, ?> item = getItem(customModelData);
		if (item == null) return null;
		return item.variant_of(stack);
	}

	public CustomItem<? extends Module<?>, ? extends CustomItem<? extends Module<?>, ?>> getItem(Class<?> clazz) {
		return instances.get(clazz);
	}

	public static class ReverseLookupEntry extends ReverseLookupEntryParent {

		public CustomItem<?, ?> custom_item;
		public ItemVariantEnum variant;

		public ReverseLookupEntry(CustomItem<?, ?> custom_item, ItemVariantEnum variant) {
			super(EntryType.Variant);
			this.custom_item = custom_item;
			this.variant = variant;
		}
	}

	public enum EntryType {
		Variant,
		Icon,
	}

	public static class ReverseLookupEntryParent {

		private EntryType type;

		public ReverseLookupEntryParent(EntryType type) {
			this.type = type;
		}
	}
}
