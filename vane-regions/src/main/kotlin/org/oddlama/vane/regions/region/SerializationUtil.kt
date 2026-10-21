package org.oddlama.vane.regions.region

import java.util.*
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer

/** JSON key used for serialized UUID id fields. */
private const val KEY_ID = "id"

/** JSON key used for serialized name fields. */
private const val KEY_NAME = "name"

/** JSON key used for serialized owner UUID fields. */
private const val KEY_OWNER = "owner"

/** Serializes a typed value into a JSON object field via `PersistentSerializer`. */
internal fun <T> putSerialized(json: JSONObject, key: String, clazz: Class<T>, value: T?) {
    json.put(key, PersistentSerializer.toJson(clazz, value))
}

/** Deserializes a typed value from a JSON object field via `PersistentSerializer`. */
internal fun <T> readSerialized(json: JSONObject, key: String, clazz: Class<T>): T? =
    PersistentSerializer.fromJson(clazz, json.get(key))

/** Serializes the common ownable fields (id, name, owner) shared by [Region] and [RegionGroup]. */
internal fun putOwnable(json: JSONObject, id: UUID?, name: String?, owner: UUID?) {
    putSerialized(json, KEY_ID, UUID::class.java, id)
    putSerialized(json, KEY_NAME, String::class.java, name)
    putSerialized(json, KEY_OWNER, UUID::class.java, owner)
}

/**
 * Deserializes the common ownable fields (id, name, owner) shared by [Region] and [RegionGroup].
 */
internal fun readOwnable(json: JSONObject): Triple<UUID?, String?, UUID?> {
    val id = readSerialized(json, KEY_ID, UUID::class.java)
    val name = readSerialized(json, KEY_NAME, String::class.java)
    val owner = readSerialized(json, KEY_OWNER, UUID::class.java)
    return Triple(id, name, owner)
}

/**
 * Wraps a [NoSuchFieldException]-throwing block, rethrowing as [RuntimeException] with a clear
 * message.
 */
internal inline fun <T> withField(block: () -> T): T =
    try {
        block()
    } catch (e: NoSuchFieldException) {
        throw RuntimeException("Invalid field. This is a bug.", e)
    }
