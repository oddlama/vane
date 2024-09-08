package org.oddlama.vane.core.resourcepack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.NamespacedKey;
import org.json.JSONArray;
import org.json.JSONObject;

import net.kyori.adventure.key.Key;

public class ResourcePackGenerator {

	private String description = "";
	private byte[] icon_png_content = null;
	private Map<String, Map<String, JSONObject>> translations = new HashMap<>();
	private Map<NamespacedKey, List<JSONObject>> item_overrides = new HashMap<>();
	private Map<NamespacedKey, PackEntry> item_textures = new HashMap<>();

	public void set_description(String description) {
		this.description = description;
	}

	public void set_icon_png(File file) throws IOException {
		this.icon_png_content = Files.readAllBytes(file.toPath());
	}

	public void set_icon_png(InputStream data) throws IOException {
		this.icon_png_content = data.readAllBytes();
	}

	public JSONObject translations(String namespace, String lang_code) {
		var ns = translations.computeIfAbsent(namespace, k -> new HashMap<>());
		var lang_map = ns.get(lang_code);
		if (lang_map == null) {
			lang_map = new JSONObject();
			ns.put(lang_code, lang_map);
		}
		return lang_map;
	}

	public void add_item_model(NamespacedKey key, InputStream texture_png, Key parent) throws IOException {
		item_textures.put(key, new PackEntry(texture_png.readAllBytes(), parent));
	}

	public void add_item_model(NamespacedKey key, InputStream texture_png, InputStream texture_png_mcmeta, Key parent) throws IOException {
		item_textures.put(key, new PackEntry(texture_png.readAllBytes(), texture_png_mcmeta.readAllBytes(), parent));
	}

	public void add_item_override(
		NamespacedKey base_item_key,
		NamespacedKey new_item_key,
		Consumer<JSONObject> create_predicate
	) {
		var overrides = item_overrides.computeIfAbsent(base_item_key, k -> new ArrayList<>());

		final var predicate = new JSONObject();
		create_predicate.accept(predicate);

		final var override = new JSONObject();
		override.put("predicate", predicate);
		override.put("model", new_item_key.getNamespace() + ":item/" + new_item_key.getKey());
		overrides.add(override);
	}

	private String generate_pack_mcmeta() {
		final var pack = new JSONObject();
		pack.put("pack_format", 34);
		pack.put("description", description);

		final var root = new JSONObject();
		root.put("pack", pack);

		return root.toString();
	}

	private void write_translations(final ZipOutputStream zip) throws IOException {
		for (var t : translations.entrySet()) {
			var namespace = t.getKey();
			for (var ns : t.getValue().entrySet()) {
				var lang_code = ns.getKey();
				var lang_map = ns.getValue();
				zip.putNextEntry(new ZipEntry("assets/" + namespace + "/lang/" + lang_code + ".json"));
				zip.write(lang_map.toString().getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
	}

	private JSONObject create_item_model(NamespacedKey texture, Key item_type) {
		// Create model json
		final var model = new JSONObject();

		// FIXME: hardcoded fixes. better rewrite RP generator
		// and use static files for all items. just language should be generated.
		final var textures = new JSONObject();
		if (texture.getNamespace().equals("minecraft") && texture.getKey().endsWith("shulker_box")) {
			model.put("parent", "minecraft:item/template_shulker_box");

			textures.put("particle", "minecraft:block/" + texture.getKey());
			model.put("textures", textures);
		} else {
			model.put("parent", item_type.toString());
			if (texture.getNamespace().equals("minecraft") && texture.getKey().equals("compass")) {
				textures.put("layer0", texture.getNamespace() + ":item/compass_16");
			} else {
				textures.put("layer0", texture.getNamespace() + ":item/" + texture.getKey());
			}
			model.put("textures", textures);
		}

		return model;
	}

	private void write_item_models(final ZipOutputStream zip) throws IOException {
		for (var entry : item_textures.entrySet()) {
			final var key = entry.getKey();
			final var texture_png = entry.getValue().texture_png;
			final var texture_png_mcmeta = entry.getValue().texture_png_mcmeta;

			// Write texture
			zip.putNextEntry(new ZipEntry("assets/" + key.getNamespace() + "/textures/item/" + key.getKey() + ".png"));
			zip.write(texture_png);
			zip.closeEntry();

			// Write mcmeta if given
			if (texture_png_mcmeta.length > 0) {
				zip.putNextEntry(new ZipEntry("assets/" + key.getNamespace() + "/textures/item/" + key.getKey() + ".png.mcmeta"));
				zip.write(texture_png_mcmeta);
				zip.closeEntry();
			}

			// Write model json
			final var model = create_item_model(key, entry.getValue().parent);
			zip.putNextEntry(new ZipEntry("assets/" + key.getNamespace() + "/models/item/" + key.getKey() + ".json"));
			zip.write(model.toString().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
	}

	private void write_item_overrides(final ZipOutputStream zip) throws IOException {
		for (var entry : item_overrides.entrySet()) {
			final var key = entry.getKey();

			final var overrides = new JSONArray();
			// Be sure to iterate in sorted order, as predicates must
			// be sorted in the final json.
			// Otherwise, minecraft will
			//  select the wrong items.
			entry.getValue().stream().sorted(Comparator.comparing(o -> {
				if (!o.has("predicate")) {
					return 0;
				}
				final var pred = o.getJSONObject("predicate");
				if (!pred.has("custom_model_data")) {
					return 0;
				}
				return pred.getInt("custom_model_data");
			})).forEach(overrides::put);

			// Create model json
			final var model = create_item_model(key, override_parent(key));
			model.put("overrides", overrides);

			// Write item model override
			zip.putNextEntry(new ZipEntry("assets/" + key.getNamespace() + "/models/item/" + key.getKey() + ".json"));
			zip.write(model.toString().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
		}
	}

	public void write(File file) throws IOException {
		try (var zip = new ZipOutputStream(new FileOutputStream(file))) {
			zip.putNextEntry(new ZipEntry("pack.mcmeta"));
			zip.write(generate_pack_mcmeta().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			if (icon_png_content != null) {
				zip.putNextEntry(new ZipEntry("pack.png"));
				zip.write(icon_png_content);
				zip.closeEntry();
			}

			write_translations(zip);
			write_item_models(zip);
			write_item_overrides(zip);
		} catch (IOException e) {
			throw e;
		}
	}

	class PackEntry {
		final byte[] texture_png;
		final byte[] texture_png_mcmeta;
		final Key parent;

		PackEntry(byte[] texture_png, byte[] texture_png_mcmeta, Key parent) {
			this.texture_png = texture_png;
			this.texture_png_mcmeta = texture_png_mcmeta;
			this.parent = parent;
		}

		PackEntry(byte[] texture_png, Key parent) {
			this.texture_png = texture_png;
			this.texture_png_mcmeta = new byte[0];
			this.parent = parent;
		}
	}

	/**
	 * Gives the type of parent used by the given item
	 *
	 * @param item_key
	 * @return a key containing the parent type
	 */
	private Key override_parent(NamespacedKey item_key){
		switch(item_key.getKey()){
			case "wooden_hoe":
			case "stone_hoe":
			case "iron_hoe":
			case "golden_hoe":
			case "diamond_hoe":
			case "netherite_hoe":
				return Key.key("minecraft:item/handheld");
			case "warped_fungus_on_a_stick":
				return Key.key("minecraft:item/handheld_rod");
			case "dropper":
				return Key.key("minecraft:block/dropper");
			default:
				return Key.key("minecraft:item/generated");
		}
	}
}
