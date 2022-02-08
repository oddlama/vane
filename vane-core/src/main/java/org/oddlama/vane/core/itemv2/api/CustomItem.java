package org.oddlama.vane.core.itemv2.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.util.Util;

import net.kyori.adventure.text.Component;

public interface CustomItem {
	public static final NamespacedKey CUSTOM_ITEM_IDENTIFIER = Util.namespaced_key("vane_api", "custom_item_identifier");

	public NamespacedKey key();
	public boolean enabled();

	public Material baseMaterial();
	public int customModelData();
	public @Nullable Component displayName();

	public boolean usesCustomDurability();
	public Component durabilityLore();
	public int durability();
	public boolean canMend();
	// TODO catch PlayerItemBreakEvent and PlayerItemDamageEvent to modify custom durability.
	// TODO check if handle.hurtAndBreak calls these.

	//public void onAddResources(final ResourcePackGenerator rp) {
	//}

	default public ItemStack updateItemStack(@NotNull final ItemStack itemStack) {
		return itemStack;
	}

	default public ItemStack newStack() {
		return newStack(1);
	}

	default public ItemStack newStack(final int amount) {
		final var itemStack = new ItemStack(baseMaterial(), amount);
		itemStack.editMeta(meta -> {
			meta.setCustomModelData(customModelData());
			meta.displayName(displayName());
		});
		return updateItemStack(itemStack);
	}

	default public ItemStack convertExistingStack(ItemStack itemStack) {
		itemStack = itemStack.clone();
		itemStack.setType(baseMaterial());
		itemStack.editMeta(meta -> {
			final var data = meta.getPersistentDataContainer();
			data.set(CUSTOM_ITEM_IDENTIFIER, PersistentDataType.STRING, key().toString());
			meta.setCustomModelData(customModelData());
		});
		return updateItemStack(itemStack);
	}
}
