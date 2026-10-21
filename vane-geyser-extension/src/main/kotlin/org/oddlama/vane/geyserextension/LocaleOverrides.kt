package org.oddlama.vane.geyserextension

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.geysermc.geyser.api.extension.Extension

/**
 * Deploys vane's translations into Geyser's `locales/overrides` directory, so Bedrock clients see
 * translated vane messages without the files being copied by hand.
 *
 * Geyser reads `locales/overrides/<locale>.json` when it loads a locale: `en_us` during startup and
 * every other locale when the first player using it joins. Both happen after extensions receive
 * [org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent], which is when this runs.
 *
 * An override file is shared with anything else the admin overrides, so vane's strings are merged
 * into it instead of replacing it: keys starting with [KEY_PREFIX] are vane's and are replaced by
 * the current set (dropping renamed or removed ones), every other key is kept as it was.
 */
object LocaleOverrides {
    /**
     * Classpath location of the merged per-locale translations produced by `generateBedrockPack`.
     */
    private const val RESOURCE = "/bedrock/locales.json"

    /** Prefix shared by every vane translation key (`vane_admin.`, `vane_core.`, ...). */
    private const val KEY_PREFIX = "vane_"

    private val GSON = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    /**
     * Merges the bundled translations into Geyser's override files.
     *
     * A locale whose existing override file cannot be read or parsed is skipped with a warning and
     * left untouched, so a broken file is never replaced by one that silently lost the admin's
     * keys.
     */
    fun deploy(extension: Extension) {
        val bundled =
            LocaleOverrides::class.java.getResourceAsStream(RESOURCE)
                ?: throw IOException("$RESOURCE is missing from the extension jar")
        val locales =
            bundled.reader(StandardCharsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }

        val dir = extension.geyserApi().configDirectory().resolve("locales").resolve("overrides")
        Files.createDirectories(dir)

        var updated = 0
        for ((locale, strings) in locales.entrySet()) {
            val file = dir.resolve("$locale.json")
            try {
                if (merge(file, strings.asJsonObject)) updated++
            } catch (e: Exception) {
                if (e !is IOException && e !is JsonParseException && e !is IllegalStateException)
                    throw e
                extension
                    .logger()
                    .warning(
                        "Not deploying vane translations to '$file', it could not be read: ${e.message}"
                    )
            }
        }
        extension
            .logger()
            .info(
                "Deployed vane translations for ${locales.size()} locales ($updated updated) to $dir"
            )
    }

    /** Merges [vaneStrings] into [file]. Returns whether the file changed. */
    private fun merge(file: Path, vaneStrings: JsonObject): Boolean {
        val existing =
            if (Files.exists(file)) {
                val text = String(Files.readAllBytes(file), StandardCharsets.UTF_8)
                if (text.isBlank()) JsonObject() else JsonParser.parseString(text).asJsonObject
            } else {
                JsonObject()
            }

        val merged = JsonObject()
        existing
            .entrySet()
            .filterNot { it.key.startsWith(KEY_PREFIX) }
            .forEach { merged.add(it.key, it.value) }
        vaneStrings.entrySet().forEach { merged.add(it.key, it.value) }
        if (Files.exists(file) && merged == existing) return false

        // Write to a temporary file first, so a crash mid-write never leaves a truncated override
        // file.
        val tmp = file.resolveSibling("${file.fileName}.tmp")
        Files.write(tmp, (GSON.toJson(merged) + "\n").toByteArray(StandardCharsets.UTF_8))
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return true
    }
}
