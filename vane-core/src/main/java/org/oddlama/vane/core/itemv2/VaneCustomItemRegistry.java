package org.oddlama.vane.core.itemv2;

import java.util.HashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.itemv2.api.CustomItem;
import org.oddlama.vane.core.itemv2.api.CustomItemRegistry;

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
		return CustomItemHelper
	}

	@Override
	public void register(final CustomItem customItem) {
	}

	@Override
	public void removePermanently(final NamespacedKey key) {
	}
}