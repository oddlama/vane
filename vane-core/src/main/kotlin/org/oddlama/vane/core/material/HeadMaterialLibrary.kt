package org.oddlama.vane.core.material

import org.bukkit.NamespacedKey
import org.json.JSONArray
import org.oddlama.vane.core.material.HeadMaterial.Companion.from

/** Registry of known custom head materials loaded from JSON data. */
object HeadMaterialLibrary {
    /** Head registry by namespaced key. */
    private val registry = mutableMapOf<NamespacedKey, HeadMaterial>()

    /** Category lookup map. */
    private val categories = mutableMapOf<String, MutableList<HeadMaterial>>()

    /** Tag lookup map. */
    private val tags = mutableMapOf<String, MutableList<HeadMaterial>>()

    /** Lookup by base64 texture payload. */
    private val byTexture = mutableMapOf<String, HeadMaterial>()

    /** Loads head materials from serialized JSON array content. */
    @JvmStatic
    fun load(string: String) {
        val json = JSONArray(string)
        for (i in 0 until json.length()) {
            val mat = from(json.getJSONObject(i))

            registry[mat.key!!] = mat
            byTexture[mat.texture!!] = mat

            categories.getOrPut(mat.category!!) { mutableListOf() }.add(mat)

            mat.tags.forEach { tag ->
                tags.getOrPut(tag!!) { mutableListOf() }.add(mat)
            }
        }
    }

    /** Resolves a head material by key. */
    @JvmStatic fun from(key: NamespacedKey?): HeadMaterial? = registry[key]

    /** Resolves a head material by texture payload. */
    @JvmStatic fun fromTexture(base64Texture: String?): HeadMaterial? = byTexture[base64Texture]

    /** Returns all loaded head materials. */
    @JvmStatic fun all(): Collection<HeadMaterial> = registry.values
}
