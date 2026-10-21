package org.oddlama.vane.core.resourcepack

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONObject

/** Builds a distributable resource pack zip from collected language JSON and additional files. */
class ResourcePackGenerator {
    /** Collected translation files grouped by namespace and language code. */
    private val translations: MutableMap<String, MutableMap<String, JSONObject>> = mutableMapOf()

    /** Additional raw files to include in the generated zip, keyed by zip path. */
    private val files: MutableMap<String, ByteArray> = mutableMapOf()

    /**
     * Returns or creates the translation JSON object for a namespace/language pair.
     *
     * @param namespace the namespace under `assets`.
     * @param langCode the language code, e.g. `en_us`.
     * @return the mutable translation JSON object.
     */
    fun translations(namespace: String, langCode: String): JSONObject {
        val ns = translations.getOrPut(namespace) { mutableMapOf() }
        return ns.getOrPut(langCode) { JSONObject() }
    }

    /**
     * Writes all collected translation JSON objects into the zip stream.
     *
     * @param zip the zip output stream.
     * @throws IOException if writing fails.
     */
    @Throws(IOException::class)
    private fun writeTranslations(zip: ZipOutputStream) {
        for ((namespace, map) in translations) {
            for ((langCode, langMap) in map) {
                zip.putNextEntry(ZipEntry("assets/$namespace/lang/$langCode.json"))
                zip.write(langMap.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    /**
     * Reads a file stream and adds it to the generated archive.
     *
     * @param path the file path inside the zip.
     * @param stream the input stream to read.
     * @throws IOException if reading fails.
     */
    @Throws(IOException::class)
    fun addFile(path: String, stream: InputStream) {
        files[path] = stream.readBytes()
    }

    /**
     * Writes the resource pack archive to a file.
     *
     * @param file the destination zip file.
     * @throws IOException if writing fails.
     */
    @Throws(IOException::class)
    fun write(file: File) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            writeTranslations(zip)
            for ((path, content) in files) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content)
                zip.closeEntry()
            }
        }
    }
}
