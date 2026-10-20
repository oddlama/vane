package org.oddlama.vane.core.resourcepack

import java.util.*

/**
 * Resolution of the UUID a resource pack is sent with.
 */
object ResourcePackUuid {
    /** Returns [uuid] parsed, or `null` if it is blank or malformed. */
    fun parse(uuid: String?): UUID? {
        if (uuid.isNullOrBlank()) return null
        return try {
            UUID.fromString(uuid.trim())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * Returns a UUID derived from [url], for packs configured without one. It stays the same
     * across restarts, so clients keep recognizing the pack they already downloaded.
     */
    fun fromUrl(url: String): UUID = UUID.nameUUIDFromBytes(url.toByteArray(Charsets.UTF_8))
}
