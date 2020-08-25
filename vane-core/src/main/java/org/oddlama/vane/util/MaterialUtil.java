package org.oddlama.vane.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

public class MaterialUtil {
	public static Material material_from(NamespacedKey key) {
		return Registry.MATERIAL.get(key);
	}

	public static boolean is_seed(Material type) {
		switch (type) {
			default:
				return false;

			case WHEAT:
			case CARROTS:
			case POTATOES:
			case BEETROOTS:
			case NETHER_WART:
				return true;
		}
	}
}
