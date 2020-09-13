package org.oddlama.vane.core.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.NamespacedKey;

import org.json.JSONArray;

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
