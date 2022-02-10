package org.oddlama.vane.core.itemv2;

import java.util.HashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.itemv2.api.CustomItem;
import org.oddlama.vane.core.itemv2.api.CustomItemRegistry;
import org.oddlama.vane.core.itemv2.api.CustomModelDataRegistry;

public class VaneCustomItemRegistry implements CustomItemRegistry {
	private final HashMap<NamespacedKey, CustomItem> items = new HashMap<>();

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
		//if (!core.custom_items().model_data_registry().has())
		// TODO core.custom_items().model_data_registry().registerUsage()
		// TODO core.custom_items().model_data_registry().removeUsage()
	}

	@Override
	public void removePermanently(final NamespacedKey key) {
	}
}
