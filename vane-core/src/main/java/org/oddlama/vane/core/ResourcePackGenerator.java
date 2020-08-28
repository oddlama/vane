package org.oddlama.vane.core;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import java.nio.file.Files;
import org.bukkit.permissions.PermissionDefault;

import org.json.simple.JSONObject;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Message;

public class ResourcePackGenerator {
	private String description = "";
	private byte[] icon_png_content = null;
	private Map<String, Map<String, JSONObject>> translations = new HashMap<>();

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
		var ns = translations.get(namespace);
		if (ns == null) {
			ns = new HashMap<String, JSONObject>();
			translations.put(namespace, ns);
		}
		var lang_map = ns.get(lang_code);
		if (lang_map == null) {
			lang_map = new JSONObject();
			ns.put(lang_code, lang_map);
		}
		return lang_map;
	}

	@SuppressWarnings("unchecked")
	private String generate_pack_mcmeta() {
		final var pack = new JSONObject();
		pack.put("pack_format", 6);
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
		} catch (IOException e) {
			throw e;
		}
	}
}
