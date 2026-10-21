package org.oddlama.vane.util

import java.util.*
import org.bukkit.Bukkit
import org.bukkit.block.Block

/**
 * Lazily resolves block references from stored world UUID and coordinates.
 *
 * @param worldId world UUID used for lazy world lookup.
 * @param x block x coordinate.
 * @param y block y coordinate.
 * @param z block z coordinate.
 */
class LazyBlock(
    /** World UUID used for lazy resolution. */
    val worldId: UUID?,
    /** Block x coordinate. */
    val x: Int,
    /** Block y coordinate. */
    val y: Int,
    /** Block z coordinate. */
    val z: Int,
) {
    /** Cached resolved block instance. */
    private var block: Block? = null

    /** Creates a lazy block from an existing block reference. */
    constructor(
        block: Block?
    ) : this(
        worldId = block?.world?.uid,
        x = block?.x ?: 0,
        y = block?.y ?: 0,
        z = block?.z ?: 0,
    ) {
        this.block = block
    }

    /** Returns the resolved block, loading world/block lazily when needed. */
    fun block(): Block? {
        if (worldId != null && block == null) {
            block = Bukkit.getWorld(worldId)!!.getBlockAt(x, y, z)
        }
        return block
    }
}
