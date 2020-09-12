package org.oddlama.vane.core.material;

import static org.oddlama.vane.util.ItemUtil.skull_with_texture;
import static org.oddlama.vane.util.Util.namespaced_key;

import java.util.List;
import java.util.ArrayList;
import org.bukkit.NamespacedKey;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.json.JSONArray;
import org.json.JSONObject;

public class HeadMaterial {
	private NamespacedKey key;
	private String name;
	private String category;
	private List<String> tags;
	private String base64_texture;

	public HeadMaterial(final NamespacedKey key, final String name, final String category, final List<String> tags, final String base64_texture) {
		this.key = key;
		this.name = name;
		this.category = category;
		this.tags = tags;
		this.base64_texture = base64_texture;
	}

	public NamespacedKey key() { return key; }
	public String name() { return name; }
	public String category() { return category; }
	public List<String> tags() { return tags; }
	public String texture() { return base64_texture; }

	public ItemStack item() {
		return skull_with_texture(name, base64_texture);
	}

	public static HeadMaterial from(final JSONObject json) {
		final var id = json.getString("id");
		final var name = json.getString("name");
		final var category = json.getString("category");
		final var texture = json.getString("texture");

		final var tags = new ArrayList<String>();
		final var tags_arr = json.getJSONArray("tags");
		for (int i = 0; i < tags_arr.length(); ++i) {
			tags.add(tags_arr.getString(i));
		}

		final var key = namespaced_key("vane", category + "_" + id);
		return new HeadMaterial(key, name, category, tags, texture);
	}
}
