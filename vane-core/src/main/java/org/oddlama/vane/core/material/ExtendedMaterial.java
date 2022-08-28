package org.oddlama.vane.core.material;

import static org.oddlama.vane.util.MaterialUtil.material_from;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.item.api.CustomItem;

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

	public boolean is_simple_material() {
		return material != null;
	}

	public static ExtendedMaterial from(final NamespacedKey key) {
		final var mat = new ExtendedMaterial(key);
		if (mat.material == null && mat.head_material == null && key.namespace().equals("minecraft")) {
			// If no material was found and the key doesn't suggest a custom item, return null.
			return null;
		}
		return mat;
	}

	public static ExtendedMaterial from(final Material material) {
		return from(material.getKey());
	}

	public static ExtendedMaterial from(final CustomItem customItem) {
		return from(customItem.key());
	}

	public ItemStack item() {
		return item(1);
	}

	public ItemStack item(int amount) {
		if (head_material != null) {
			final var item = head_material.item();
			item.setAmount(amount);
			return item;
		}
		if (material != null) {
			return new ItemStack(material, amount);
		}

		final var custom_item = Core.instance().item_registry().get(key);
		if (custom_item == null) {
			throw new IllegalStateException("ExtendedMaterial '" + key + "' is neither a classic material, a head nor a custom item!");
		}

		return custom_item.newStack();
	}
}
