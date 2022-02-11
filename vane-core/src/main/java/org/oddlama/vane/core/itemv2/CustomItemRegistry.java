package org.oddlama.vane.core.itemv2;

import java.util.HashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.itemv2.api.CustomItem;
import org.oddlama.vane.core.itemv2.api.CustomModelDataRegistry;

public class CustomItemRegistry implements org.oddlama.vane.core.itemv2.api.CustomItemRegistry {
	private final HashMap<NamespacedKey, CustomItem> items = new HashMap<>();
	private CustomModelDataRegistry model_data_registry;

	public CustomItemRegistry(final CustomModelDataRegistry model_data_registry) {
		this.model_data_registry = model_data_registry;
	}

	@Override
	public @Nullable boolean has(final NamespacedKey resourceKey) {
		return items.containsKey(resourceKey);
	}

	@Override
	public @Nullable CustomItem get(final NamespacedKey resourceKey) {
		return items.get(resourceKey);
	}

	@Override
	public @Nullable CustomItem get(final ItemStack itemStack) {
		final var key_and_version = CustomItemHelper.customItemTagsFromItemStack(itemStack);
		if (key_and_version == null) {
			return null;
		}

		return get(key_and_version.getLeft());
	}

	@Override
	public void register(final CustomItem customItem) {
		model_data_registry.reserveSingle(customItem.key(), customItem.customModelData());
		if (has(customItem.key())) {
			throw new IllegalArgumentException("A custom item with the same key '" + customItem.key() + "' has already been registered");
		}
		items.put(customItem.key(), customItem);
	}

	@Override
	public void removePermanently(final NamespacedKey key) {
	}

	@Override
	public CustomModelDataRegistry dataRegistry() {
		return model_data_registry;
	}
}
