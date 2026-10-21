package org.oddlama.vane.portals.portal

import java.io.IOException
import java.util.*
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer

/**
 * Lightweight lookup payload that maps a block to a portal id and block type.
 *
 * @property portalId id of the owning portal.
 * @property type semantic type of the portal block.
 */
class PortalBlockLookup(private val portalId: UUID?, private val type: PortalBlock.Type?) {
    /** Returns the owning portal id. */
    fun portalId() = portalId

    /** Returns the semantic block type. */
    fun type() = type

    /** JSON serializer helpers for [PortalBlockLookup]. */
    companion object {
        /** Serializes a portal block lookup into a JSON object. */
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val lookup = o as PortalBlockLookup
            val json = JSONObject()
            json.put("portalId", PersistentSerializer.toJson(UUID::class.java, lookup.portalId))
            json.put("type", PersistentSerializer.toJson(PortalBlock.Type::class.java, lookup.type))
            return json
        }

        /** Deserializes a portal block lookup from a JSON object. */
        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): PortalBlockLookup {
            val json = o as JSONObject
            val portalId: UUID? =
                PersistentSerializer.fromJson(UUID::class.java, json.get("portalId"))
            val type: PortalBlock.Type? =
                PersistentSerializer.fromJson(PortalBlock.Type::class.java, json.get("type"))
            return PortalBlockLookup(portalId, type)
        }
    }
}
