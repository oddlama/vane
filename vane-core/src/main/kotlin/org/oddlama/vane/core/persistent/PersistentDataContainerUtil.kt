package org.oddlama.vane.core.persistent

import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.json.JSONObject
import java.io.IOException
import java.util.*

/**
 * Returns all UUIDs stored under keys that start with [prefix] in this [PersistentDataContainer].
 * Useful for loading/saving collections of serialized objects keyed by UUID.
 */
fun PersistentDataContainer.storedUuidsByPrefix(prefix: String): Set<UUID> =
    keys
        .filter { it.toString().startsWith(prefix) }
        .map { UUID.fromString(it.toString().substring(prefix.length)) }
        .toSet()

/**
 * Reads a complete, non-mutating snapshot of UUID-keyed JSON records. A bad key, wrong PDC type,
 * malformed JSON or decoder failure rejects the snapshot instead of silently skipping a record.
 */
@Throws(IOException::class)
fun <T : Any> PersistentDataContainer.readStoredJsonRecords(
    prefix: String,
    decode: (UUID, JSONObject) -> T,
): Map<UUID, T> {
    val records = linkedMapOf<UUID, T>()
    for (key in keys.filter { it.toString().startsWith(prefix) }) {
        try {
            val id = UUID.fromString(key.toString().substring(prefix.length))
            // Otherwise distinct keys can collapse to one ID and later cleanup addresses the wrong key.
            require(key.toString() == prefix + id) { "Non-canonical record UUID" }
            val bytes = requireNotNull(get(key, PersistentDataType.BYTE_ARRAY)) { "Missing or wrong-typed record" }
            records[id] = decode(id, JSONObject(String(bytes, Charsets.UTF_8)))
        } catch (e: Exception) {
            throw IOException("Cannot load persistent record '$key'", e)
        }
    }
    return records
}
