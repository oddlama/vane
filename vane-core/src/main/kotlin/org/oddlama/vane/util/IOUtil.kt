package org.oddlama.vane.util

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import org.json.JSONException
import org.json.JSONObject

/** I/O helpers. */
object IOUtil {
    /**
     * Connect and read timeout for [readJsonFromUrl], so a stalled remote cannot block a thread
     * forever.
     */
    private const val TIMEOUT_MS = 10_000

    /** Fetches JSON content from a URL and parses it as a [JSONObject]. */
    @JvmStatic
    @Throws(IOException::class, JSONException::class, URISyntaxException::class)
    fun readJsonFromUrl(url: String): JSONObject {
        val connection =
            URI(url).toURL().openConnection().apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
        return connection.getInputStream().bufferedReader(StandardCharsets.UTF_8).use { rd ->
            JSONObject(rd.readText())
        }
    }
}
