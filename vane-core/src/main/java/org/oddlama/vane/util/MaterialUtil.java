package org.oddlama.vane.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

public class MaterialUtil {

	public static Material material_from(NamespacedKey key) {
		return Registry.MATERIAL.get(key);
	}

	public static boolean is_seeded_plant(Material type) {
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

	public static Material seed_for(Material plant_type) {
		switch (plant_type) {
			default:
				return null;
			case WHEAT:
				return Material.WHEAT_SEEDS;
			case CARROTS:
				return Material.CARROT;
			case POTATOES:
				return Material.POTATO;
			case BEETROOTS:
				return Material.BEETROOT_SEEDS;
			case NETHER_WART:
				return Material.NETHER_WART;
		}
	}

	public static Material farmland_for(Material seed_type) {
		switch (seed_type) {
			default:
				return null;
			case WHEAT_SEEDS:
			case CARROT:
			case POTATO:
			case BEETROOT_SEEDS:
				return Material.FARMLAND;
			case NETHER_WART:
				return Material.SOUL_SAND;
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
			case DIRT_PATH:
				return true;
		}
	}
}
