package org.oddlama.vane.portals.portal

import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.util.Vector

/**
 * Orientation of a portal plane normal.
 *
 * @property plane portal plane corresponding to this orientation.
 * @property vector unit direction vector for this orientation.
 */
enum class Orientation(private val plane: Plane, private val vector: Vector) {
    POSITIVE_X(Plane.YZ, Vector(1, 0, 0)),
    NEGATIVE_X(Plane.YZ, Vector(-1, 0, 0)),
    POSITIVE_Y(Plane.XZ, Vector(0, 1, 0)),
    NEGATIVE_Y(Plane.XZ, Vector(0, -1, 0)),
    POSITIVE_Z(Plane.XY, Vector(0, 0, 1)),
    NEGATIVE_Z(Plane.XY, Vector(0, 0, -1));

    /** Returns the portal plane corresponding to this orientation. */
    fun plane() = plane

    /** Returns a clone of the orientation unit vector. */
    fun vector() = vector.clone()

    /** Returns a component mask used for orientation projections. */
    fun componentMask() = vector().multiply(vector)

    /** Returns the opposite orientation. */
    fun flip() =
        when (this) {
            NEGATIVE_X -> POSITIVE_X
            POSITIVE_X -> NEGATIVE_X
            NEGATIVE_Z -> POSITIVE_Z
            POSITIVE_Z -> NEGATIVE_Z
            NEGATIVE_Y -> POSITIVE_Y
            POSITIVE_Y -> NEGATIVE_Y
        }

    /** Applies this orientation transform to [location] relative to [reference]. */
    fun apply(
        reference: Orientation,
        location: Location,
        flipSourceIfNotOpposing: Boolean,
    ): Location {
        val l = location.clone()
        l.direction = apply(reference, location.direction, flipSourceIfNotOpposing)
        return l
    }

    /** Applies this orientation transform to [vector] relative to [reference]. */
    fun apply(reference: Orientation, vector: Vector, flipSourceIfNotOpposing: Boolean): Vector {
        val x = vector.getX()
        val y = vector.getY()
        val z = vector.getZ()

        var effectiveSource = this
        val cmask = componentMask()
        val opposing = (this.vector.dot(cmask) < 0) != (vector.dot(cmask) < 0)
        if (flipSourceIfNotOpposing && opposing) {
            effectiveSource = effectiveSource.flip()
        }

        when (effectiveSource) {
            NEGATIVE_X ->
                return when (reference) {
                    NEGATIVE_X -> Vector(-x, y, -z)
                    POSITIVE_X -> Vector(x, y, z)
                    NEGATIVE_Z -> Vector(z, y, -x)
                    POSITIVE_Z -> Vector(-z, y, x)
                    NEGATIVE_Y -> Vector(y, -x, z)
                    POSITIVE_Y -> Vector(-y, x, z)
                }

            POSITIVE_X ->
                return when (reference) {
                    NEGATIVE_X -> Vector(x, y, z)
                    POSITIVE_X -> Vector(-x, y, -z)
                    NEGATIVE_Z -> Vector(-z, y, x)
                    POSITIVE_Z -> Vector(z, y, -x)
                    NEGATIVE_Y -> Vector(-y, x, z)
                    POSITIVE_Y -> Vector(y, -x, z)
                }

            NEGATIVE_Z ->
                return when (reference) {
                    NEGATIVE_X -> Vector(-z, y, x)
                    POSITIVE_X -> Vector(z, y, -x)
                    NEGATIVE_Z -> Vector(-x, y, -z)
                    POSITIVE_Z -> Vector(x, y, z)
                    NEGATIVE_Y -> Vector(x, -z, y)
                    POSITIVE_Y -> Vector(-x, z, y)
                }

            POSITIVE_Z ->
                return when (reference) {
                    NEGATIVE_X -> Vector(z, y, -x)
                    POSITIVE_X -> Vector(-z, y, x)
                    NEGATIVE_Z -> Vector(x, y, z)
                    POSITIVE_Z -> Vector(-x, y, -z)
                    NEGATIVE_Y -> Vector(-x, z, y)
                    POSITIVE_Y -> Vector(x, -z, y)
                }

            NEGATIVE_Y ->
                return when (reference) {
                    NEGATIVE_X -> Vector(-y, 0.0, 0.0)
                    POSITIVE_X -> Vector(y, 0.0, 0.0)
                    NEGATIVE_Z -> Vector(0.0, 0.0, -y)
                    POSITIVE_Z -> Vector(0.0, 0.0, y)
                    NEGATIVE_Y -> Vector(x, -y, z)
                    POSITIVE_Y -> Vector(x, y, z)
                }

            POSITIVE_Y ->
                return when (reference) {
                    NEGATIVE_X -> Vector(y, 0.0, 0.0)
                    POSITIVE_X -> Vector(-y, 0.0, 0.0)
                    NEGATIVE_Z -> Vector(0.0, 0.0, y)
                    POSITIVE_Z -> Vector(0.0, 0.0, -y)
                    NEGATIVE_Y -> Vector(x, y, z)
                    POSITIVE_Y -> Vector(-x, -y, -z)
                }
        }
    }

    /** Factory methods for deriving orientation from portal geometry. */
    companion object {
        /** Derives portal orientation from plane, origin, console, and player location. */
        @JvmStatic
        fun from(
            plane: Plane,
            origin: Block,
            console: Block,
            entityLocation: Location,
        ): Orientation {
            when (plane) {
                Plane.XY -> {
                    val originZ = origin.z + 0.5
                    val consoleZ = console.z + 0.5
                    return if (consoleZ > originZ) {
                        NEGATIVE_Z
                    } else if (consoleZ < originZ) {
                        POSITIVE_Z
                    } else {
                        if (entityLocation.z > originZ) {
                            NEGATIVE_Z
                        } else {
                            POSITIVE_Z
                        }
                    }
                }

                Plane.YZ -> {
                    val originX = origin.x + 0.5
                    val consoleX = console.x + 0.5
                    return if (consoleX > originX) {
                        NEGATIVE_X
                    } else if (consoleX < originX) {
                        POSITIVE_X
                    } else {
                        if (entityLocation.x > originX) {
                            NEGATIVE_X
                        } else {
                            POSITIVE_X
                        }
                    }
                }

                Plane.XZ -> {
                    val originY = origin.y + 0.5
                    val consoleY = console.y + 0.5
                    return if (consoleY >= originY) {
                        NEGATIVE_Y
                    } else { // if (consoleY < originY)
                        POSITIVE_Y
                    }
                }
            }
        }
    }
}
