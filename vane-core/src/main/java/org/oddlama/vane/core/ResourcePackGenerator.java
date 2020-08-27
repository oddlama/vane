package org.oddlama.vane.core;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

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
	private int format = 6;
	private String description = "";
	private File icon = null;

	public void set_description(String description) {
		this.description = description;
	}

	public void set_icon_png(File icon) {
		this.icon = icon;
	}

	@SuppressWarnings("unchecked")
	private String generate_pack_mcmeta() {
		final var pack = new JSONObject();
		pack.put("pack_format", format);
		pack.put("description", description);

		final var root = new JSONObject();
		root.put("pack", pack);

		return root.toString();
	}

	public void write(File file) {
		try (var zip = new ZipOutputStream(new FileOutputStream(file))) {
			zip.putNextEntry(new ZipEntry("pack.mcmeta"));
			zip.write(generate_pack_mcmeta().getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();

			if (icon != null) {
				zip.putNextEntry(new ZipEntry("pack.png"));
				zip.write(Files.readString(icon.toPath()).getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		} catch (IOException e) {
		}
	}
}
