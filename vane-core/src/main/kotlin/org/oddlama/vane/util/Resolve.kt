package org.oddlama.vane.util

import java.io.IOException
import java.net.URISyntaxException
import java.util.*
import org.json.JSONException
import org.oddlama.vane.util.IOUtil.readJsonFromUrl

/** Mojang profile resolution helpers. */
object Resolve {
    /** The five groups of a Mojang UUID as returned undashed by the profile API. */
    private val UNDASHED_UUID =
        Regex("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)")

    /** Resolves signed skin texture data for a player UUID. */
    @JvmStatic
    @Throws(IOException::class, JSONException::class, URISyntaxException::class)
    fun resolveSkin(id: UUID?): Skin {
        val json =
            readJsonFromUrl(
                "https://sessionserver.mojang.com/session/minecraft/profile/$id?unsigned=false"
            )
        val obj = json.getJSONArray("properties").getJSONObject(0)
        return Skin(
            texture = obj.getString("value"),
            signature = obj.getString("signature"),
        )
    }

    /** Resolves a player name to a Mojang UUID. */
    @JvmStatic
    @Throws(IOException::class, JSONException::class, URISyntaxException::class)
    fun resolveUuid(name: String): UUID {
        val json = readJsonFromUrl("https://api.mojang.com/users/profiles/minecraft/$name")
        val uuidStr =
            json
                .getString("id")
                .replace(
                    UNDASHED_UUID,
                    "$1-$2-$3-$4-$5",
                )
        return UUID.fromString(uuidStr)
    }

    /**
     * Skin texture payload returned by Mojang profile endpoints.
     *
     * @param texture base64 texture payload.
     * @param signature Mojang signature for the texture payload.
     */
    data class Skin(
        @JvmField var texture: String? = null,
        @JvmField var signature: String? = null,
    )
}
