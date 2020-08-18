package org.oddlama.vane.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

public class MaterialUtil {
	public static Material material_from(NamespacedKey key) {
		return Registry.MATERIAL.get(key);
	}
}
