package org.oddlama.vane.core.material;

import static org.oddlama.vane.util.MaterialUtil.material_from;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public class ExtendedMaterial {

	private NamespacedKey key;
	private Material material;
	private HeadMaterial head_material;

	private ExtendedMaterial(final NamespacedKey key) {
		this.key = key;
		this.material = material_from(key);
		if (this.material == null) {
			this.head_material = HeadMaterialLibrary.from(key);
		} else {
			this.head_material = null;
		}
	}

	public NamespacedKey key() {
		return key;
	}

	public static ExtendedMaterial from(final NamespacedKey key) {
		final var mat = new ExtendedMaterial(key);
		if (mat.material == null && mat.head_material == null) {
			return null;
		}
		return mat;
	}

	public static ExtendedMaterial from(final Material material) {
		return from(material.getKey());
	}

	public ItemStack item() {
		return item(1);
	}

	public ItemStack item(int amount) {
		if (material == null) {
			final var item = head_material.item();
			item.setAmount(amount);
			return item;
		} else {
			return new ItemStack(material, amount);
		}
	}
}
