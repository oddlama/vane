package org.oddlama.vane.core.itemv2.api;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

public interface CustomItem {
	public NamespacedKey resourceKey();
	public boolean overrideDurability();
	public int minDurability();
	public int maxDurability();
	public Material baseMaterial();
}
