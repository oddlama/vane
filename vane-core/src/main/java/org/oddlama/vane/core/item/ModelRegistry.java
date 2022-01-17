package org.oddlama.vane.core.item;

import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.module.Module;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ModelRegistry {
	// Track instances
	private static final Map<Class<?>, CustomItem<?, ?>> instances = new HashMap<>();
	// Reverse lookup model data -> custom item class, variant
	static final Map<Integer, CustomItem.ReverseLookupEntry> reverse_lookup = new HashMap<>();

	public <V extends CustomItem<T, V>, T extends Module<T>> void register(Class<? extends CustomItem> clazz, CustomItem item) {
		instances.put(clazz, item);
	}

	public boolean is_registered(Class<? extends CustomItem> clazz) {
		return instances.get(clazz) != null;
	}

	public <V extends CustomItem<T, V>, T extends Module<T>, U extends ItemVariantEnum>
	void registerVariant(CustomItem item, CustomItemVariant<T, V, U> v, U variant) {
		ModelRegistry.reverse_lookup.put(v.model_data(), new CustomItem.ReverseLookupEntry(item, variant));
	}

	/**
	 * Asserts that there is no other item with the same model data
	 */
	protected final <T extends Module<T>,V extends CustomItem<T, V>>
	void check_valid_model_data(CustomItem<T,V> parent, CustomItemVariant<T, V, ?> variant) {
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
			item.get_module()
				.log.log(
					Level.SEVERE,
					"Missing enum entry for " + getClass() + ", must be called '" + item.name().toUpperCase() + "'"
				);
			throw e;
		}
	}

	public <V extends CustomItem<?, V>> CustomItem<?, ?> getItem(int customModelData) {
		final CustomItem.ReverseLookupEntry entry = reverse_lookup.get(customModelData);
		if (entry == null) return null;
		return entry.custom_item;
	}

	public <V extends CustomItem<?, V>> CustomItemVariant<?, V, ?> getVariant(int customModelData, ItemStack stack) {
		final CustomItem<?, ?> item = getItem(customModelData);
		if(item == null) return null;
		return item.variant_of(stack);
	}

	public CustomItem<? extends Module<?>, ? extends CustomItem<? extends Module<?>, ?>> getItem(Class<?> clazz) {
		return instances.get(clazz);
	}
}
