package org.oddlama.vane.regions.region

import java.io.IOException
import java.util.*
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.block.Block
import org.json.JSONObject
import org.oddlama.vane.core.persistent.PersistentSerializer
import org.oddlama.vane.util.LazyBlock

/** Inclusive 3D extent used for region geometry checks and indexing. */
class RegionExtent {
    // Both inclusive, so we don't run into errors with
    // blocks outside the world (y<min_height || y>max_height).
    // Also, coordinates are sorted, so min is always the smaller coordinate on each axis.
    // For each x,y,z: min.[x,y,z] <= max.[x,y,z]
    /** Minimum inclusive corner. */
    private val min: LazyBlock // inclusive

    /** Maximum inclusive corner. */
    private val max: LazyBlock // inclusive

    /** Creates an extent from pre-sorted lazy corners. */
    constructor(min: LazyBlock, max: LazyBlock) {
        this.min = min
        this.max = max
    }

    /** Creates an extent from two blocks and normalizes coordinate ordering. */
    constructor(from: Block, to: Block) {
        if (from.world != to.world) {
            throw RuntimeException("Invalid region extent across dimensions!")
        }

        // Sort coordinates along axes.
        this.min =
            LazyBlock(
                from.world.getBlockAt(
                    kotlin.math.min(from.x, to.x),
                    kotlin.math.min(from.y, to.y),
                    kotlin.math.min(from.z, to.z),
                )
            )
        this.max =
            LazyBlock(
                from.world.getBlockAt(
                    kotlin.math.max(from.x, to.x),
                    kotlin.math.max(from.y, to.y),
                    kotlin.math.max(from.z, to.z),
                )
            )
    }

    /** Returns world UUID of this extent. */
    fun world(): UUID? = min.worldId

    /** Resolves and returns minimum corner block. */
    fun min(): Block? = min.block()

    /** Resolves and returns maximum corner block. */
    fun max(): Block? = max.block()

    /** Returns whether a location lies inside this extent. */
    fun isInside(loc: Location): Boolean {
        val l = min() ?: return false
        val h = max() ?: return false
        return loc.world == l.world &&
            loc.x >= l.x &&
            loc.x < (h.x + 1) &&
            loc.y >= l.y &&
            loc.y < (h.y + 1) &&
            loc.z >= l.z &&
            loc.z < (h.z + 1)
    }

    /** Returns whether a block lies inside this extent. */
    fun isInside(block: Block): Boolean {
        val l = min() ?: return false
        val h = max() ?: return false
        return block.world == l.world &&
            block.x >= l.x &&
            block.x <= h.x &&
            block.y >= l.y &&
            block.y <= h.y &&
            block.z >= l.z &&
            block.z <= h.z
    }

    /** Returns whether this extent intersects another extent. */
    fun intersectsExtent(other: RegionExtent): Boolean {
        val l1 = min() ?: return false
        val h1 = max() ?: return false
        val l2 = other.min() ?: return false
        val h2 = other.max() ?: return false
        if (l1.world != l2.world) return false

        // Compute global min and max for each axis
        val llx = kotlin.math.min(l1.x, l2.x)
        val lly = kotlin.math.min(l1.y, l2.y)
        val llz = kotlin.math.min(l1.z, l2.z)
        val hhx = kotlin.math.max(h1.x, h2.x)
        val hhy = kotlin.math.max(h1.y, h2.y)
        val hhz = kotlin.math.max(h1.z, h2.z)

        // Compute global extent length
        val extentGlobalX = (hhx - llx) + 1
        val extentGlobalY = (hhy - lly) + 1
        val extentGlobalZ = (hhz - llz) + 1

        // Compute a sum of local extent lengths
        val extentSumX = (h2.x - l2.x) + (h1.x - l1.x) + 2
        val extentSumY = (h2.y - l2.y) + (h1.y - l1.y) + 2
        val extentSumZ = (h2.z - l2.z) + (h1.z - l1.z) + 2

        // It intersects exactly when:
        //   for all and in axis: global_extent(a) < individual_extent_sum(a)
        return extentGlobalX < extentSumX &&
            extentGlobalY < extentSumY &&
            extentGlobalZ < extentSumZ
    }

    /** Returns whether this extent intersects a chunk. */
    fun intersectsChunk(chunk: Chunk): Boolean {
        val l1 = min() ?: return false
        val h1 = max() ?: return false
        if (chunk.world != l1.world) return false
        val l2x = chunk.x * 16
        val l2z = chunk.z * 16
        val h2x = (chunk.x + 1) * 16 - 1
        val h2z = (chunk.z + 1) * 16 - 1

        // Compute global min and max for each axis
        val llx = kotlin.math.min(l1.x, l2x)
        val llz = kotlin.math.min(l1.z, l2z)
        val hhx = kotlin.math.max(h1.x, h2x)
        val hhz = kotlin.math.max(h1.z, h2z)

        // Compute global extent length
        val extentGlobalX = (hhx - llx) + 1
        val extentGlobalZ = (hhz - llz) + 1

        // Compute a sum of local extent lengths
        val extentSumX = (h2x - l2x) + (h1.x - l1.x) + 2
        val extentSumZ = (h2z - l2z) + (h1.z - l1.z) + 2

        // It intersects exactly when:
        //   for all and in axis: global_extent(a) < individual_extent_sum(a)
        return extentGlobalX < extentSumX && extentGlobalZ < extentSumZ
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        /** Serializes a region extent to JSON-compatible data. */
        fun serialize(o: Any): Any {
            val regionExtent = o as RegionExtent
            val json = JSONObject()
            json.put("min", PersistentSerializer.toJson(LazyBlock::class.java, regionExtent.min))
            json.put("max", PersistentSerializer.toJson(LazyBlock::class.java, regionExtent.max))
            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        /** Deserializes a region extent from JSON-compatible data. */
        fun deserialize(o: Any): RegionExtent {
            val json = o as JSONObject
            val min: LazyBlock? =
                PersistentSerializer.fromJson(LazyBlock::class.java, json.get("min"))
            val max: LazyBlock? =
                PersistentSerializer.fromJson(LazyBlock::class.java, json.get("max"))
            return RegionExtent(requireNotNull(min), requireNotNull(max))
        }
    }
}
