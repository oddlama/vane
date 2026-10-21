package org.oddlama.vane.core.persistent

import java.io.IOException
import java.lang.reflect.Field
import org.json.JSONObject

/**
 * Reflection-backed persistent storage binding for a single field.
 *
 * @param owner object that owns the reflected field.
 * @param field reflected persistent field.
 * @param mapName maps Java field names to storage keys.
 */
class PersistentField(
    private val owner: Any?,
    private val field: Field,
    mapName: (String?) -> String?,
) {
    /** Persistent storage path for this field. */
    private val path: String? = mapName(field.name.substring("storage".length))

    init {
        field.isAccessible = true
    }

    /** Returns the persistent storage path. */
    fun path(): String? = path

    /** Returns the current reflected field value. */
    fun get(): Any? =
        try {
            field.get(owner)
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }

    /** Serializes this field into the given JSON object. */
    @Throws(IOException::class)
    fun save(json: JSONObject) {
        json.put(path, PersistentSerializer.toJson(field, get()) ?: JSONObject.NULL)
    }

    /** Loads this field value from the given JSON object. */
    @Throws(IOException::class)
    fun load(json: JSONObject) {
        if (!json.has(path)) throw IOException("Missing key in persistent storage: '$path'")
        try {
            field.set(owner, PersistentSerializer.fromJson(field, json.get(path)))
        } catch (_: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.")
        }
    }
}
