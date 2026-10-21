package org.oddlama.vane.portals.portal

import java.io.IOException
import java.util.*
import org.bukkit.block.Block
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.util.LazyBlock
import org.oddlama.vane.util.LegacyKeys

/**
 * Represents a typed block belonging to a portal structure.
 *
 * @property block lazy block reference.
 * @property type semantic role of the block in the portal.
 */
class PortalBlock(private val block: LazyBlock, private val type: Type?) {
    /** Creates a portal block from a Bukkit block instance. */
    constructor(block: Block?, type: Type?) : this(LazyBlock(block), type)

    /** Resolves and returns the referenced Bukkit block. */
    fun block() = block.block()

    /** Returns the semantic portal block type. */
    fun type() = type

    /** Creates a lightweight lookup payload for this block and [portalId]. */
    fun lookup(portalId: UUID?) = PortalBlockLookup(portalId, type)

    /** Returns hash code based on block identity. */
    override fun hashCode() = block().hashCode()

    /** Returns true when [other] refers to the same block position. */
    override fun equals(other: Any?): Boolean {
        return other is PortalBlock && block() == other.block()

        // Only block is compared, as the same block can only have one functions.
    }

    /** Semantic role of a block inside portal structures. */
    enum class Type {
        ORIGIN,
        CONSOLE,
        BOUNDARY1,
        BOUNDARY2,
        BOUNDARY3,
        BOUNDARY4,
        BOUNDARY5,
        PORTAL;

        companion object {
            /**
             * Returns the type serialized as [name]. Releases before v1.22.0 wrote the boundary
             * types as `BOUNDARY_1` .. `BOUNDARY_5`, which are still found in portal records and
             * style maps.
             */
            @JvmStatic
            fun fromSerialized(name: String): Type =
                entries.firstOrNull { it.name == name }
                    ?: entries.firstOrNull { LegacyKeys.matches(name, it.name) }
                    ?: throw IllegalArgumentException("Invalid portal block type '$name'")
        }
    }

    /** JSON serializer helpers for [PortalBlock]. */
    companion object {
        /** Serializes a portal block into a JSON object. */
        @JvmStatic
        @Throws(IOException::class)
        fun serialize(o: Any): Any {
            val portalBlock = o as PortalBlock
            val json = JSONObject()
            json.put("block", PersistentSerializer.toJson(LazyBlock::class.java, portalBlock.block))
            json.put("type", PersistentSerializer.toJson(Type::class.java, portalBlock.type))
            return json
        }

        /** Deserializes a portal block from a JSON object. */
        @JvmStatic
        @Throws(IOException::class)
        fun deserialize(o: Any): PortalBlock {
            val json = o as JSONObject
            // `PersistentSerializer.fromJson` signature: `fun <U> fromJson(cls: Class<U>?, value:
            // Any?): U?`.
            // Do not include `?` in the generic parameter; the return type is already nullable.
            val block: LazyBlock? =
                PersistentSerializer.fromJson(LazyBlock::class.java, json.get("block"))
            val type: Type? = PersistentSerializer.fromJson(Type::class.java, json.get("type"))
            return PortalBlock(block!!, type)
        }
    }
}
