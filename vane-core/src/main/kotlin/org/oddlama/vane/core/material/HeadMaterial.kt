package org.oddlama.vane.core.material

import java.util.Base64
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.json.JSONObject
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.StorageUtil.namespacedKey

/**
 * Data model for a predefined custom player-head material entry.
 *
 * @param key namespaced key identifier.
 * @param name display name.
 * @param category category label.
 * @param tags tag list.
 * @param texture base64 texture payload.
 */
class HeadMaterial(
    /** Unique namespaced identifier. */
    val key: NamespacedKey?,
    /** Display name of this head entry. */
    val name: String?,
    /** Category label grouping this head. */
    val category: String?,
    tags: List<String?>,
    /** Base64 texture payload. */
    val texture: String?,
) {
    /** Unique tag set for this head material. */
    val tags: Set<String?> = tags.toHashSet()

    /** Creates an item stack representing this head material. */
    fun item(): ItemStack = ItemUtil.skullWithTexture(name!!, texture!!)

    /** JSON deserialization helpers. */
    companion object {
        /** Every head texture payload in the library points at this host. */
        private const val TEXTURE_URL_PREFIX = "https://textures.minecraft.net/texture/"

        /**
         * Rebuilds the base64 texture payload for a bare texture hash.
         *
         * Almost every entry in `head_library.json` used to store ~180 characters of base64 that
         * decoded to nothing but this fixed wrapper around a ~64 character hash, which made the
         * texture field roughly half the 11.7 MB file. Entries now carry the hash alone and the
         * payload is rebuilt here; the few whose payload carries extra fields keep it verbatim
         * under `texture` instead. The output is byte-identical to what was stored before, which
         * matters because [HeadMaterialLibrary.fromTexture] matches skulls on the exact string.
         *
         * @param hash Texture hash as it appears at the end of the texture URL.
         * @return The base64 payload for that hash.
         */
        private fun textureFromHash(hash: String): String =
            Base64.getEncoder()
                .encodeToString(
                    """{"textures":{"SKIN":{"url":"$TEXTURE_URL_PREFIX$hash"}}}"""
                        .toByteArray(Charsets.UTF_8)
                )

        @JvmStatic
        /** Creates a [HeadMaterial] from a serialized JSON object. */
        fun from(json: JSONObject): HeadMaterial {
            val id = json.getString("id")
            val name = json.getString("name")
            val category = json.getString("category")
            // `hash` is the compact form; `texture` is the verbatim fallback.
            val texture =
                if (json.has("texture")) json.getString("texture")
                else textureFromHash(json.getString("hash"))
            val tagsArr = json.getJSONArray("tags")
            val tags = (0 until tagsArr.length()).map { tagsArr.getString(it) }
            val key = namespacedKey("vane", "${category}_$id")
            return HeadMaterial(key, name, category, tags, texture)
        }
    }
}
