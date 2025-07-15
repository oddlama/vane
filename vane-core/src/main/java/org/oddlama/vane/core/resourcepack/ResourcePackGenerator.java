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
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.json.JSONArray;
import org.json.JSONObject;

public class ResourcePackGenerator {
    private Map<String, Map<String, JSONObject>> translations = new HashMap<>();
    private Map<String, byte[]> files = new HashMap<>();

    public JSONObject translations(String namespace, String lang_code) {
        var ns = translations.computeIfAbsent(namespace, k -> new HashMap<>());
        var lang_map = ns.get(lang_code);
        if (lang_map == null) {
            lang_map = new JSONObject();
            ns.put(lang_code, lang_map);
        }
        return lang_map;
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

	public void add_file(String path, InputStream stream) throws IOException {
		files.put(path, stream.readAllBytes());
	}

    public void write(File file) throws IOException {
        try (var zip = new ZipOutputStream(new FileOutputStream(file))) {
            write_translations(zip);

			// Add all files
            for (var f : files.entrySet()) {
                var path = f.getKey();
                var content = f.getValue();
                zip.putNextEntry(new ZipEntry(path));
                zip.write(content);
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw e;
        }
    }
}
