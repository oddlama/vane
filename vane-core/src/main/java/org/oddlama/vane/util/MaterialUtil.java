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

	public static boolean is_replaceable_grass(Material type) {
		switch (type) {
			default:
				return false;

			case TALL_GRASS:
			case GRASS:
				return true;
		}
	}

	public static boolean is_tillable(Material type) {
		switch (type) {
			default:
				return false;

			case DIRT:
			case GRASS_BLOCK:
				return true;
		}
	}

}
