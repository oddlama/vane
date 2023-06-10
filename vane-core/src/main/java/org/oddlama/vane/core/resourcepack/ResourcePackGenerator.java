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

import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.NamespacedKey;
import org.json.JSONArray;
import org.json.JSONObject;

public class ResourcePackGenerator {

	private String description = "";
	private byte[] icon_png_content = null;
	private Map<String, Map<String, JSONObject>> translations = new HashMap<>();
	private Map<NamespacedKey, List<JSONObject>> item_overrides = new HashMap<>();
	private Map<NamespacedKey, Pair<byte[], byte[]>> item_textures = new HashMap<>();

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

	public void add_item_model(NamespacedKey key, InputStream texture_png) throws IOException {
		item_textures.put(key, Pair.of(texture_png.readAllBytes(), null));
	}

	public void add_item_model(NamespacedKey key, InputStream texture_png, InputStream texture_png_mcmeta) throws IOException {
		item_textures.put(key, Pair.of(texture_png.readAllBytes(), texture_png_mcmeta.readAllBytes()));
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
		pack.put("pack_format", 15);
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

	private JSONObject create_item_model_handheld(NamespacedKey texture) {
		// Create model json
		final var model = new JSONObject();

		// FIXME: hardcoded fixes. better rewrite RP generator
		// and use static files for all items. just language should be generated.
		if (texture.getNamespace().equals("minecraft") && texture.getKey().equals("dropper")) {
			model.put("parent", "minecraft:block/dropper");
		} else if (texture.getNamespace().equals("minecraft") && texture.getKey().endsWith("shulker_box")) {
			model.put("parent", "minecraft:item/template_shulker_box");
			final var textures = new JSONObject();
			textures.put("particle", "minecraft:block/" + texture.getKey());
			model.put("textures", textures);
		} else {
			model.put("parent", "minecraft:item/handheld");
			final var textures = new JSONObject();
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
			final var texture_png = entry.getValue().getLeft();
			final var texture_png_mcmeta = entry.getValue().getRight();

			// Write texture
			zip.putNextEntry(new ZipEntry("assets/" + key.getNamespace() + "/textures/item/" + key.getKey() + ".png"));
			zip.write(texture_png);
			zip.closeEntry();

			// Write mcmeta if given
			if (texture_png_mcmeta != null) {
				zip.putNextEntry(new ZipEntry("assets/" + key.getNamespace() + "/textures/item/" + key.getKey() + ".png.mcmeta"));
				zip.write(texture_png_mcmeta);
				zip.closeEntry();
			}

			// Write model json
			final var model = create_item_model_handheld(key);
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
			// be sorted in the final json. otherwise minecraft will
			// select wrong items.
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
			final var model = create_item_model_handheld(key);
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
}
