package org.oddlama.vane.core.itemv2.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import net.kyori.adventure.text.Component;

public abstract class SimpleCustomItem implements CustomItem {
	public NamespacedKey key;
	public Material baseMaterial;
	public int customModelData;
	public Component displayName;

	public SimpleCustomItem(final NamespacedKey resourceKey, final Material baseMaterial, final int customModelData, final Component displayName) {
		this.key = resourceKey;
		this.baseMaterial = baseMaterial;
		this.customModelData = customModelData;
		this.displayName = displayName;
	}

	@Override
	public NamespacedKey key() {
		return key;
	}

	@Override
	public Material baseMaterial() {
		return baseMaterial;
	}

	@Override
	public int customModelData() {
		return customModelData;
	}

	// TODO return Component.translatable("vane:item_test").decoration(TextDecoration.ITALIC, false);
	@Override
	public Component displayName() {
		return displayName;
	}

	@Override
	public int durability() {
		return 0;
	}

	//public void addResources(final ResourcePackGenerator rp) {
	//}
}
