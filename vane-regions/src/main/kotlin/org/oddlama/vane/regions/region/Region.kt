package org.oddlama.vane.regions.region

import java.io.IOException
import java.util.*
import org.json.JSONObject
import org.oddlama.vane.regions.Regions

/** Persistent region entity containing ownership, extent, and group assignment. */
class Region {
    private constructor()

    /** Creates a new region with a generated id. */
    constructor(name: String?, owner: UUID?, extent: RegionExtent?, regionGroup: UUID?) {
        this.id = UUID.randomUUID()
        this.name = name
        this.owner = owner
        this.extent = extent
        this.regionGroup = regionGroup
    }

    /** Unique region identifier. */
    private var id: UUID? = null

    /** Region display name. */
    private var name: String? = null

    /** Owner player UUID. */
    private var owner: UUID? = null

    /** Spatial extent occupied by this region. */
    private var extent: RegionExtent? = null

    /** Region-group id controlling role permissions for this region. */
    private var regionGroup: UUID? = null

    @JvmField
    /** Indicates whether this region needs to be written back to persistence. */
    var invalidated: Boolean = true

    /** Returns this region's id. */
    fun id(): UUID? = id

    /** Returns this region's name. */
    fun name(): String? = name

    /** Updates this region's name and marks it dirty. */
    fun name(name: String?) {
        this.name = name
        this.invalidated = true
    }

    /** Returns the owner UUID. */
    fun owner(): UUID? = owner

    /** Returns the region extent. */
    fun extent(): RegionExtent? = extent

    /** Cached resolved region-group instance. */
    private var cachedRegionGroup: RegionGroup? = null

    /** Returns the assigned region-group id. */
    fun regionGroupId(): UUID? = regionGroup

    /** Assigns a new region-group id and resets cached references. */
    fun regionGroupId(regionGroup: UUID?) {
        this.regionGroup = regionGroup
        this.cachedRegionGroup = null
        this.invalidated = true
    }

    /** Resolves and caches the assigned region group from module storage. */
    fun regionGroup(regions: Regions): RegionGroup? =
        cachedRegionGroup ?: regions.getRegionGroup(regionGroup).also { cachedRegionGroup = it }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        /** Serializes a region to JSON-compatible structure. */
        fun serialize(o: Any): Any {
            val region = o as Region
            val json = JSONObject()
            putOwnable(json, region.id, region.name, region.owner)
            putSerialized(json, "regionGroup", UUID::class.java, region.regionGroup)
            putSerialized(json, "extent", RegionExtent::class.java, region.extent)
            return json
        }

        @JvmStatic
        @Throws(IOException::class)
        /** Deserializes a region from JSON-compatible structure. */
        fun deserialize(o: Any): Region {
            val json = o as JSONObject
            val region = Region()
            val (id, name, owner) = readOwnable(json)
            region.id = id
            region.name = name
            region.owner = owner
            region.regionGroup = readSerialized(json, "regionGroup", UUID::class.java)
            region.extent = readSerialized(json, "extent", RegionExtent::class.java)
            return region
        }
    }
}
