package org.oddlama.vane.core.itemv2.api;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * This is the registry with which you can register your custom items.
 */
public interface CustomItemRegistry {
	/**
	 * Returns true if a custom item with the given resourceKey has been registered.
	 */
	public @Nullable boolean has(NamespacedKey resourceKey);

	/**
	 * Tries to retrieve a custom item definition by resource key.
	 * Returns null if no such definition exists.
	 */
	public @Nullable CustomItem get(NamespacedKey resourceKey);

	/**
	 * Tries to retrieve a custom item definition from an ItemStack.
	 * Returns null if the itemstack is not a custom item, or references a custom item
	 * that has not been registered (e.g. previously installed plugin).
	 */
	// TODO: make command /clearcustomitems namespace:key that queues an item for deletion even if the original plugin is gone now. Maybe even allow clearing a whole namespace.
	// TODO: for an immediate operation on a whole world, NBTExplorer can be used together with a removal filter filtering on the custom item id.
	public @Nullable CustomItem get(ItemStack itemStack);

	/**
	 * Registers a new custom item. Throws an IllegalArgumentException if an
	 * item with the same key has already been registered.
	 */
	public void register(CustomItem customItem);

	/**
	 * Queues removal of a given custom item. If any matching item is encountered
	 * in the future, it will be removed permanently from the respective inventory.
	 *
	 * This is not a one-off operation! Removal only actually happens when the item
	 * is encountered due to a player interacting with an inventory. This is intended
	 * as a way for plugins to queue removal of items from old plugin versions.
	 */
	public void removePermanently(NamespacedKey key);
}
