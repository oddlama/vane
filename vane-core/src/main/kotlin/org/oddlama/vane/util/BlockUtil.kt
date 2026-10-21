package org.oddlama.vane.util

import net.minecraft.core.BlockPos
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Skull
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.entity.LivingEntity
import org.bukkit.inventory.ItemStack
import org.bukkit.util.BlockVector
import org.bukkit.util.Vector

/** Block and raytrace utility helpers. */
object BlockUtil {
    /** All six cardinal block faces. */
    @JvmField
    val BLOCK_FACES: List<BlockFace?> =
        listOf(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN,
        )

    /** Horizontal block faces on the XZ plane. */
    @JvmField
    val XZ_FACES: List<BlockFace> =
        listOf(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
        )

    /** Maximum supported radius for precomputed nearest-block lookup. */
    const val NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX: Int = 6

    /** Precomputed nearest relative block vectors per radius. */
    @JvmField
    val NEAREST_RELATIVE_BLOCKS_FOR_RADIUS: List<List<BlockVector>> =
        (0..NEAREST_RELATIVE_BLOCKS_FOR_RADIUS_MAX).map { nearestBlocksForRadius(it) }

    /** Returns whether two blocks share the same coordinates. */
    fun equalsPos(b1: Block, b2: Block): Boolean = b1.x == b2.x && b1.y == b2.y && b1.z == b2.z

    /** Drops an item naturally at a block center. */
    @JvmStatic
    fun dropNaturally(block: Block, drop: ItemStack) =
        dropNaturally(block.location.add(0.5, 0.5, 0.5), drop)

    /** Drops an item naturally at a location with random position and velocity offsets. */
    @JvmStatic
    fun dropNaturally(loc: Location, drop: ItemStack) {
        loc.world
            .dropItem(
                loc.add(Vector.getRandom().subtract(Vector(.5, .5, .5)).multiply(0.5)),
                drop,
            )
            .velocity = Vector.getRandom().add(Vector(-.5, +.5, -.5)).normalize().multiply(.15)
    }

    /** Returns nearby relative blocks in a radius, sorted by distance. */
    fun nearestBlocksForRadius(radius: Int): List<BlockVector> = buildList {
        for (x in -radius..radius) {
            for (z in -radius..radius) {
                if (x * x + z * z <= radius * radius + 0.5) {
                    add(BlockVector(x, 0, z))
                }
            }
        }
        sortWith(BlockVectorRadiusComparator())
    }

    /** Resolves a relative block from a root block and vector offset. */
    @JvmStatic
    fun relative(block: Block, relative: Vector): Block =
        block.getRelative(relative.blockX, relative.blockY, relative.blockZ)

    /** Finds the nearest tillable block around a root block. */
    @JvmStatic
    fun nextTillableBlock(rootBlock: Block, radius: Int, careless: Boolean): Block? {
        for (relativePos in NEAREST_RELATIVE_BLOCKS_FOR_RADIUS[radius]) {
            val block = relative(rootBlock, relativePos)
            if (!MaterialUtil.isTillable(block.type)) continue
            val above = block.getRelative(0, 1, 0)
            when {
                above.type == Material.AIR -> return block
                careless && MaterialUtil.isReplaceableGrass(above.type) -> {
                    above.type = Material.AIR
                    return block
                }
            }
        }
        return null
    }

    /** Returns direct, edge, and corner adjacent blocks around a root block. */
    @JvmStatic
    fun adjacentBlocks3D(root: Block): Array<Block?> =
        arrayOf(
            // Direct adjacent
            root.getRelative(1, 0, 0),
            root.getRelative(-1, 0, 0),
            root.getRelative(0, 0, 1),
            root.getRelative(0, 0, -1),
            root.getRelative(0, 1, 0),
            root.getRelative(0, -1, 0),
            // Edge adjacent
            root.getRelative(0, -1, -1),
            root.getRelative(0, -1, 1),
            root.getRelative(0, 1, -1),
            root.getRelative(0, 1, 1),
            root.getRelative(-1, 0, -1),
            root.getRelative(-1, 0, 1),
            root.getRelative(1, 0, -1),
            root.getRelative(1, 0, 1),
            root.getRelative(-1, -1, 0),
            root.getRelative(-1, 1, 0),
            root.getRelative(1, -1, 0),
            root.getRelative(1, 1, 0),
            // Corner adjacent
            root.getRelative(-1, -1, -1),
            root.getRelative(-1, -1, 1),
            root.getRelative(-1, 1, -1),
            root.getRelative(-1, 1, 1),
            root.getRelative(1, -1, -1),
            root.getRelative(1, -1, 1),
            root.getRelative(1, 1, -1),
            root.getRelative(1, 1, 1),
        )

    /** Finds the nearest air block above matching farmland around a root block. */
    @JvmStatic
    fun nextSeedableBlock(rootBlock: Block, farmlandType: Material?, radius: Int): Block? {
        for (relativePos in NEAREST_RELATIVE_BLOCKS_FOR_RADIUS[radius]) {
            val block = relative(rootBlock, relativePos)
            val below = block.getRelative(BlockFace.DOWN)
            if (below.type == farmlandType && block.type == Material.AIR) return block
        }
        return null
    }

    /** Triggers neighbor updates for a lever-like block state change. */
    @JvmStatic
    fun updateLever(block: Block, face: BlockFace) {
        val level = (block.world as CraftWorld).handle
        val connectedBlock = block.getRelative(face.oppositeFace)
        val blockPos1 = BlockPos(block.x, block.y, block.z)
        val blockPos2 = BlockPos(connectedBlock.x, connectedBlock.y, connectedBlock.z)
        val initiatorBlock = level.getBlockIfLoaded(blockPos1)!!
        level.updateNeighborsAt(blockPos1, initiatorBlock)
        level.updateNeighborsAt(blockPos2, initiatorBlock)
    }

    /** Raytraces a block hit and returns octant information. */
    @JvmStatic
    fun raytraceOct(entity: LivingEntity, block: Block?): Oct? {
        val result = entity.rayTraceBlocks(10.0) ?: return null
        if (block == null || block != result.hitBlock) return null

        val blockMiddle = block.location.toVector().add(Vector(0.5, 0.5, 0.5))
        val hit =
            result.hitPosition
                .subtract(blockMiddle)
                .subtract(result.hitBlockFace!!.direction.multiply(0.25))
        return Oct(hit, result.hitBlockFace)
    }

    /** Raytraces and returns the dominant horizontal face around the hit point. */
    @JvmStatic
    fun raytraceDominantFace(entity: LivingEntity, block: Block?): RaytraceDominantFaceResult? {
        val result = entity.rayTraceBlocks(10.0) ?: return null
        if (block == null || block != result.hitBlock) return null

        val blockMiddle = block.location.toVector().add(Vector(0.5, 0.5, 0.5))
        val diff = result.hitPosition.subtract(blockMiddle)

        val ret = RaytraceDominantFaceResult()
        for (face in XZ_FACES) {
            val faceDominance = if (face.modX != 0) diff.x * face.modX else diff.z * face.modZ
            if (faceDominance > ret.dominance) {
                ret.face = face
                ret.dominance = faceDominance
            }
        }
        return ret
    }

    /** Extracts a base64 texture payload from a skull profile. */
    @JvmStatic
    fun textureFromSkull(skull: Skull): String? =
        skull.profile?.properties()?.firstOrNull { it.name == "textures" }?.value

    /** Encodes one of eight block-space corners. */
    class Corner
    private constructor(
        private val x: Boolean,
        private val y: Boolean,
        private val z: Boolean,
        private val id: Int,
    ) {
        /** Creates a corner from explicit axis signs. */
        constructor(
            x: Boolean,
            y: Boolean,
            z: Boolean,
        ) : this(
            x,
            y,
            z,
            ((if (x) 1 else 0) shl 2) or ((if (y) 1 else 0) shl 1) or (if (z) 1 else 0),
        )

        /** Creates a corner from a hit vector relative to block center. */
        constructor(hit: Vector) : this(hit.x >= 0.0, hit.y >= 0.0, hit.z >= 0.0)

        /** Returns whether the corner is on the upper half. */
        fun up(): Boolean = y

        /** Returns whether the corner is on the eastern half. */
        fun east(): Boolean = x

        /** Returns whether the corner is on the southern half. */
        fun south(): Boolean = z

        /** Returns a copy with updated upper/lower half. */
        fun up(up: Boolean): Corner = Corner(x, up, z)

        /** Returns a copy with updated east/west half. */
        fun east(east: Boolean): Corner = Corner(east, y, z)

        /** Returns a copy with updated north/south half. */
        fun south(south: Boolean): Corner = Corner(x, y, south)

        /** Rotates this corner into north-facing reference orientation. */
        fun rotateToNorthReference(rotation: BlockFace): Corner =
            when (rotation) {
                BlockFace.NORTH -> Corner(x, y, z)
                BlockFace.EAST -> Corner(z, y, !x)
                BlockFace.SOUTH -> Corner(!x, y, !z)
                BlockFace.WEST -> Corner(!z, y, x)
                else ->
                    throw IllegalArgumentException(
                        "rotation must be one of NORTH, EAST, SOUTH, WEST"
                    )
            }

        /** Returns the diagonal horizontal face represented by this corner. */
        fun xzFace(): BlockFace =
            when {
                x && z -> BlockFace.SOUTH_EAST
                x && !z -> BlockFace.NORTH_EAST
                !x && z -> BlockFace.SOUTH_WEST
                else -> BlockFace.NORTH_WEST
            }

        override fun hashCode(): Int = id

        override fun equals(other: Any?): Boolean = other is Corner && other.id == id
    }

    /** Raytrace octant result. */
    class Oct(private val hitPos: Vector, private val face: BlockFace?) {
        /** Corner represented by this hit position. */
        val corner: Corner = Corner(hitPos)

        /** Returns hit position relative vector. */
        fun hitPos(): Vector = hitPos

        /** Returns hit corner. */
        fun corner(): Corner = corner

        /** Returns hit face. */
        fun face(): BlockFace? = face
    }

    /** Result holder for dominant horizontal raytrace face checks. */
    class RaytraceDominantFaceResult {
        /** Dominant face candidate. */
        @JvmField var face: BlockFace? = null

        /** Dominance score for [face]. */
        @JvmField var dominance: Double = 0.0
    }

    /** Comparator sorting block vectors by squared distance to origin. */
    class BlockVectorRadiusComparator : Comparator<BlockVector> {
        /** Compares vectors by squared Euclidean distance. */
        override fun compare(a: BlockVector, b: BlockVector): Int =
            (a.blockX * a.blockX + a.blockY * a.blockY + a.blockZ * a.blockZ) -
                (b.blockX * b.blockX + b.blockY * b.blockY + b.blockZ * b.blockZ)
    }
}
