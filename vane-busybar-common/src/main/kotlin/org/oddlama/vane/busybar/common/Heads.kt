package org.oddlama.vane.busybar.common

import org.json.JSONException
import org.json.JSONObject
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * Player faces for the Bar, cut from skins the server already knows.
 *
 * A skin is identified by the hash at the end of its textures.minecraft.net URL. Only skins of
 * players seen on this server or proxy can be requested, so `/head` cannot be used to fetch arbitrary
 * URLs. Rendered faces are cached in memory.
 */
class Heads {
    /** Skin URL per skin id, for players seen since the server started. */
    private val known = ConcurrentHashMap<String, URL>()

    /** Rendered 8x8 face PNGs per skin id. */
    private val faces = ConcurrentHashMap<String, ByteArray>()

    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    /** Remembers the skin at [url] and returns its id, or null without a skin (offline mode). */
    fun remember(url: URL?): String? {
        url ?: return null
        val id = url.path.substringAfterLast('/').takeIf { SKIN_ID.matches(it) } ?: return null
        if (known.size >= MAX_ENTRIES) known.clear()
        known[id] = url
        return id
    }

    /**
     * The face for skin [id] as a 8x8 PNG, downloading the skin on first use. Blocks, so call it
     * from an HTTP handler thread, never the main thread. Returns null for unknown ids.
     *
     * @throws IOException when the skin cannot be downloaded or decoded.
     */
    fun face(id: String): ByteArray? {
        faces[id]?.let { return it }
        val url = known[id] ?: return null
        val request = HttpRequest.newBuilder(url.toURI()).timeout(Duration.ofSeconds(10)).GET().build()
        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofByteArray())
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Interrupted while downloading skin $id", e)
        }
        if (response.statusCode() != 200) throw IOException("Skin $id answered ${response.statusCode()}")
        val png = renderFace(response.body())
        if (faces.size >= MAX_ENTRIES) faces.clear()
        faces[id] = png
        return png
    }

    companion object {
        /**
         * The skin URL in a game profile's `textures` property, the base64 JSON the session server
         * signs, or null when it has none or is malformed.
         */
        fun skinUrl(textures: String?): URL? {
            textures ?: return null
            return try {
                val json = JSONObject(String(Base64.getDecoder().decode(textures)))
                val url = json.optJSONObject("textures")?.optJSONObject("SKIN")?.optString("url") ?: return null
                URI(url).toURL()
            } catch (_: IllegalArgumentException) {
                null
            } catch (_: JSONException) {
                null
            } catch (_: URISyntaxException) {
                null
            } catch (_: MalformedURLException) {
                null
            }
        }

        /** Skin ids are hex hashes. */
        private val SKIN_ID = Regex("[0-9a-f]{16,128}")

        /** Entries kept before a cache is reset. */
        private const val MAX_ENTRIES = 512

        /**
         * Cuts the face from a skin texture: the 8x8 face at (8, 8) with the hat layer at
         * (40, 8) drawn over it. Both old 64x32 and current 64x64 skins keep them there.
         * Like the game, an old 64x32 skin's hat layer is ignored when it has no transparent pixel:
         * such skins often filled it with a solid color.
         */
        fun renderFace(skinPng: ByteArray): ByteArray {
            val skin = ImageIO.read(ByteArrayInputStream(skinPng)) ?: throw IOException("Skin is not an image")
            if (skin.width < 64 || skin.height < 16) throw IOException("Unexpected skin size ${skin.width}x${skin.height}")
            val face = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
            val g = face.createGraphics()
            try {
                g.drawImage(skin.getSubimage(8, 8, 8, 8), 0, 0, null)
                val hat = skin.getSubimage(40, 8, 8, 8)
                val legacy = skin.height < 64
                val hatHasTransparency = (0 until 8).any { x -> (0 until 8).any { y -> hat.getRGB(x, y) ushr 24 < 255 } }
                if (!legacy || hatHasTransparency) g.drawImage(hat, 0, 0, null)
            } finally {
                g.dispose()
            }
            return ByteArrayOutputStream().also { ImageIO.write(face, "png", it) }.toByteArray()
        }
    }
}
