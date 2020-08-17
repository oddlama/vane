package org.oddlama.imex.util;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;

public class MaterialUtil {
	public static Material material_from(NamespacedKey key) {
		return Registry.MATERIAL.get(key);
	}
}
