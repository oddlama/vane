package org.oddlama.vane.proxycore.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import org.json.JSONException
import org.json.JSONObject

/** I/O helpers used by proxy-core utility code. */
object IOUtil {
    /**
     * Reads all text from [rd].
     *
     * @param rd reader source.
     * @return full text content.
     * @throws IOException when reading fails.
     */
    @Throws(IOException::class) private fun readAll(rd: Reader): String = rd.readText()

    /**
     * Downloads and parses JSON from [url].
     *
     * @param url URI string to fetch.
     * @return parsed JSON object.
     * @throws IOException when the resource cannot be read.
     * @throws JSONException when JSON parsing fails.
     * @throws URISyntaxException when [url] is not a valid URI.
     */
    @JvmStatic
    @Throws(IOException::class, JSONException::class, URISyntaxException::class)
    fun readJsonFromUrl(url: String): JSONObject =
        BufferedReader(InputStreamReader(URI(url).toURL().openStream(), StandardCharsets.UTF_8))
            .use { rd -> JSONObject(readAll(rd)) }
}
