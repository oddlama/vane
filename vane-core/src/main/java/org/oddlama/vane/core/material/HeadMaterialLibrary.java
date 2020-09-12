package org.oddlama.vane.core.material;

import static org.oddlama.vane.util.ItemUtil.skull_with_texture;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONObject;
import org.bukkit.NamespacedKey;

public class HeadMaterialLibrary {
	private static final Map<NamespacedKey, HeadMaterial> registry = new HashMap<>();

	public static void load(final String string) {
		final var json = new JSONObject(string);
		for (final var key : json.keySet()) {
			final var mat = HeadMaterial.from(json.getJSONObject(key));
			registry.put(mat.key(), mat);
		}
	}

	public static HeadMaterial from(final NamespacedKey key) {
		return registry.get(key);
	}
}
