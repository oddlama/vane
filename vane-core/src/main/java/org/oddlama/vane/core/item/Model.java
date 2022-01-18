package org.oddlama.vane.core.item;

import org.bukkit.inventory.ItemStack;

/**
 * @param <T> The CustomItem/Variant etc that this model represents.
 */
public interface Model<T> {
	/**
	 * @return creates an ItemStack representing this model.
	 * implementors may customize the data.
	 */
	ItemStack item();

	/**
	 * @return the NBT int value registered for this item.
	 */
	int custom_model_data();
}
