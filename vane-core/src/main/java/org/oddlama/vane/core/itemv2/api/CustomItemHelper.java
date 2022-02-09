package org.oddlama.vane.core.itemv2.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.util.Util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

public class CustomItemHelper {
	/**
	 * Used in persistent item storage to identify custom items.
	 */
	public static final NamespacedKey CUSTOM_ITEM_IDENTIFIER = Util.namespaced_key("vane_api", "custom_item_identifier");
	/**
	 * Used in persistent item storage to store custom item version.
	 */
	public static final NamespacedKey CUSTOM_ITEM_VERSION = Util.namespaced_key("vane_api", "custom_item_version");

	/**
	 * Internal function. Used as a dispatcher to update internal information and then call
	 * {@link #updateItemStack(ItemStack)} to let the user update information. This prevents
	 * problems with information de-sync in case the user would forget to call super.
	 */
	public static ItemStack updateItemStack(final CustomItem customItem, @NotNull final ItemStack itemStack) {
		itemStack.editMeta(meta -> {
			final var data = meta.getPersistentDataContainer();
			data.set(CUSTOM_ITEM_IDENTIFIER, PersistentDataType.STRING, customItem.key().toString());
			data.set(CUSTOM_ITEM_VERSION, PersistentDataType.INTEGER, customItem.version());
			meta.setCustomModelData(customItem.customModelData());
		});

		return customItem.onUpdateItemStack(itemStack);
	}

	/**
	 * Creates a new item stack with a single item of this custom item.
	 */
	public static ItemStack newStack(final CustomItem customItem) {
		return CustomItemHelper.newStack(customItem, 1);
	}

	/**
	 * Creates a new item stack with the given amount of items of this custom item.
	 */
	public static ItemStack newStack(final CustomItem customItem, final int amount) {
		final var itemStack = new ItemStack(customItem.baseMaterial(), amount);
		itemStack.editMeta(meta -> {
			meta.displayName(customItem.displayName());
		});
		return CustomItemHelper.updateItemStack(customItem, itemStack);
	}

	/**
	 * This function is called to convert an existing item stack of any
	 * form to this custom item type, without losing metadata such as name, enchantments, etc.
	 * This is for example useful to convert a diamond something into a netherite something,
	 * when those two items are different CustomItem definitions but otherwise share attributes and functionality.
	 */
	public static ItemStack convertExistingStack(final CustomItem customItem, ItemStack itemStack) {
		itemStack = itemStack.clone();
		itemStack.setType(customItem.baseMaterial());
		return CustomItemHelper.updateItemStack(customItem, itemStack);
	}
}
