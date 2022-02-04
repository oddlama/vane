package org.oddlama.vane.core.item;

import org.bukkit.inventory.ItemStack;

/**
 * A customItem with no attached mechanics.
 * Intended only for use in GUI menus.
 */
public class GUIIcon implements Model {

	@Override
	public ItemStack item() {
		return null;
	}

	@Override
	public int custom_model_data() {
		return 0;
	}
}
