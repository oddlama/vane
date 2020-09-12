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
import org.json.JSONArray;
import org.bukkit.NamespacedKey;

public class HeadMaterialLibrary {
	private static final Map<NamespacedKey, HeadMaterial> registry = new HashMap<>();
	private static final Map<String, List<HeadMaterial>> categories = new HashMap<>();
	private static final Map<String, List<HeadMaterial>> tags = new HashMap<>();

	public static void load(final String string) {
		final var json = new JSONArray(string);
		for (int i = 0; i < json.length(); ++i) {
			// Deserialize
			final var mat = HeadMaterial.from(json.getJSONObject(i));

			// Add to registry
			registry.put(mat.key(), mat);

			// Add to category lookup
			var category = categories.get(mat.category());
			if (category == null) {
				category = new ArrayList<HeadMaterial>();
				categories.put(mat.category(), category);
			}
			category.add(mat);

			// Add to tag lookup
			for (final var tag : mat.tags()) {
				var tag_list = tags.get(tag);
				if (tag_list == null) {
					tag_list = new ArrayList<HeadMaterial>();
					tags.put(tag, tag_list);
				}
				tag_list.add(mat);
			}
		}
	}

	public static HeadMaterial from(final NamespacedKey key) {
		return registry.get(key);
	}
}
