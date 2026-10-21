package org.oddlama.vane.util

import java.util.*
import org.bukkit.Bukkit
import org.bukkit.Location

/**
 * Lazily resolves world references for stored locations.
 *
 * @param worldId world UUID used for lazy world lookup.
 * @param location backing location instance.
 */
class LazyLocation(
    /** World UUID used for lazy world resolution. */
    val worldId: UUID?,
    /** Backing location value. */
    private val location: Location,
) {
    /** Creates a lazy location from an existing location. */
    constructor(
        location: Location
    ) : this(
        worldId = location.world?.uid,
        location = location.clone(),
    )

    /** Creates a lazy location from explicit coordinates and world UUID. */
    constructor(
        worldId: UUID?,
        x: Double,
        y: Double,
        z: Double,
        pitch: Float,
        yaw: Float,
    ) : this(
        worldId = worldId,
        location = Location(null, x, y, z, pitch, yaw),
    )

    /** Returns the resolved location, loading the world lazily when needed. */
    fun location(): Location {
        if (worldId != null && location.world == null) {
            location.world = Bukkit.getWorld(worldId)
        }
        return location
    }
}
