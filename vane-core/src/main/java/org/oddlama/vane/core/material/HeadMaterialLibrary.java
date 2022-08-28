package org.oddlama.vane.core.material;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.json.JSONArray;

public class HeadMaterialLibrary {

	private static final Map<NamespacedKey, HeadMaterial> registry = new HashMap<>();
	private static final Map<String, List<HeadMaterial>> categories = new HashMap<>();
	private static final Map<String, List<HeadMaterial>> tags = new HashMap<>();
	private static final Map<String, HeadMaterial> by_texture = new HashMap<>();

	public static void load(final String string) {
		final var json = new JSONArray(string);
		for (int i = 0; i < json.length(); ++i) {
			// Deserialize
			final var mat = HeadMaterial.from(json.getJSONObject(i));

			// Add to registry
			registry.put(mat.key(), mat);
			by_texture.put(mat.texture(), mat);

			// Add to category lookup
			var category = categories.computeIfAbsent(mat.category(), k -> new ArrayList<>());
			category.add(mat);

			// Add to tag lookup
			for (final var tag : mat.tags()) {
				var tag_list = tags.computeIfAbsent(tag, k -> new ArrayList<>());
				tag_list.add(mat);
			}
		}
	}

	public static HeadMaterial from(final NamespacedKey key) {
		return registry.get(key);
	}

	public static HeadMaterial from_texture(final String base64_texture) {
		return by_texture.get(base64_texture);
	}

	public static Collection<HeadMaterial> all() {
		return registry.values();
	}
}
