package org.oddlama.vane.trifles

import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Bisected
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.Door

/**
 * An abstract short-lived container to manage door states. All states are assumed from the bottom
 * half, rely on Minecraft to restore bad block states.
 */
class SingleDoor
private constructor(
    /** Lower (bottom-half) block of the door pair. */
    private val lowerBlock: Block
) {
    /** Cached lower-half door data. */
    private var lower: Door?

    /** Cached upper-half door data. */
    private var upper: Door?

    /** Assumes valid door location already. */
    init {
        this.lower = asDoorState(lowerBlock)
        this.upper = asDoorState(lowerBlock.getRelative(BlockFace.UP))
    }

    /** Returns the cached lower-half state and requires it to be present. */
    private fun lowerDoor(): Door = requireNotNull(lower)

    /**
     * Refreshes cached lower/upper states from the world.
     *
     * @return `true` if both door halves are still valid.
     */
    fun updateCachedState(): Boolean {
        // Update to the current state if possible
        val lowerData = lowerBlock.blockData
        lower = asDoorState(lowerData)
        val upperData: BlockData = otherVerticalHalf(lowerBlock).blockData
        upper = asDoorState(upperData)

        return lower != null && upper != null
    }

    /** Resolves the partner door if this door is part of a compatible double-door setup. */
    val secondDoor: SingleDoor?
        /**
         * Gets the SingleDoor instance representing the other half of this double door.
         *
         * @return null, if no matching door was found.
         */
        get() {
            // What defines a second door in minecraft?
            // Hinges *must* be (visually) on opposite sides.
            // The doors align visually.
            //
            //   N
            //  W E
            //   S
            //
            // # Single Doors
            //
            // the following is a picture of a door block from above. (built by a player facing
            // south)
            // OXXXXX // X = door hitbox
            // |    | // o = door hinge.
            // |    | // (the rest is empty)
            // ------
            // in this instance, the door is facing 'south' with the hinge in the NW corner.
            //
            // Its hinge is 'right' as it is facing south.
            // It is currently 'closed'.
            //
            // The 'open' door is strange. It retains all properties, except it's now open.
            //
            // O----- // X = door hit box
            // X | // o = door hinge.
            // X | // (the rest is empty)
            // X-----
            // in this instance, the door is *STILL* facing 'south' with the hinge in the NW corner.
            //
            // Its hinge is *still* 'right' and it is defined as facing south, even though it has
            // rotated.
            //
            // # Double Doors (built by a player facing north)
            //        1      2
            // WWWWWW ------ ------ WWWWWW // X = door hitbox
            // WWWWWW |    | |    | WWWWWW // O = door hinge.
            // WWWWWW |    | |    | WWWWWW // W = Solid block
            // WWWWWW OXXXXX XXXXXO WWWWWW // (the rest is empty)
            // Closed.
            //
            // Door 1 is
            // minecraft:acacia_door[facing=north,half=lower,hinge=left,open=false,powered=false]
            // Door 2 is
            // minecraft:acacia_door[facing=north,half=lower,hinge=right,open=false,powered=false]
            //
            //        1      2
            // WWWWWW X----- -----X WWWWWW // X = door hitbox
            // WWWWWW X    | |    X WWWWWW // O = door hinge.
            // WWWWWW X    | |    X WWWWWW // W = Solid block
            // WWWWWW O----- -----O WWWWWW // (the rest is empty)
            // Open.
            //
            // Door 1 is
            // minecraft:acacia_door[facing=north,half=lower,hinge=left,open=true,powered=false]
            // Door 2 is
            // minecraft:acacia_door[facing=north,half=lower,hinge=right,open=true,powered=false]
            //
            // # Double Doors "Hacky Tricky Type"
            //
            // Players often employ a trick when constructing doors.
            // Mobs will only navigate through open doors and will treat closed doors as unpassable.
            // It is possible to construct doors that confuse mobs (and us) to have doors that mobs
            // won't break.
            // Or ever pass through.
            //
            // ## 'closed' (but open to the player).
            //        1      2
            // WWWWWW X----- -----X WWWWWW // X = door hitbox
            // WWWWWW X    | |    X WWWWWW // O = door hinge.
            // WWWWWW X    | |    X WWWWWW // W = Solid block
            // WWWWWW O----- -----O WWWWWW // (the rest is empty)
            // Door 1:
            // /setblock 0 100 -3
            // minecraft:acacia_door[facing=east,half=lower,hinge=right,open=true,powered=false]
            // Door 2:
            // /setblock 1 100 -3
            // minecraft:acacia_door[facing=west,half=lower,hinge=left,open=true,powered=false]
            //
            //
            // ## 'open' (but closed to the player).
            //
            //        1      2
            // WWWWWW ------ ------ WWWWWW // X = door hitbox
            // WWWWWW |    | |    | WWWWWW // O = door hinge.
            // WWWWWW |    | |    | WWWWWW // W = Solid block
            // WWWWWW OXXXXX XXXXXO WWWWWW // (the rest is empty)
            //
            // Door 1:
            // /setblock 0 100 -3
            // minecraft:acacia_door[facing=east,half=lower,hinge=right,open=false,powered=false]
            // Door 2:
            // /setblock 1 100 -3
            // minecraft:acacia_door[facing=west,half=lower,hinge=left,open=false,powered=false]

            // Because of this, it is possible to construct a set of double doors, and a door can
            // simultaneously
            // belong to 2 different door sets, a 'hacked' door-set, and a normal door-set.
            // Trying to account for this would end up with a cellular automata of updates, so
            // instead
            // we prioritize
            // opening over closing.

            val normalDoor = findOtherDoor(false)
            val hackedDoor = findOtherDoor(true)

            // User testing showed that doors 'looked weird' unless you prioritized the doors that
            // 'connect' in a case of
            // conflict
            return priortize(normalDoor, hackedDoor)
        }

    /** Selects the preferred partner when normal and hacked candidates both exist. */
    private fun priortize(normalDoor: SingleDoor?, hackedDoor: SingleDoor?): SingleDoor? {
        if (normalDoor == null) return hackedDoor
        if (hackedDoor == null) return normalDoor
        if (lowerDoor().isOpen) return hackedDoor
        return normalDoor
    }

    /** Finds a matching partner door for the selected matching mode. */
    private fun findOtherDoor(hacked: Boolean): SingleDoor? {
        val lower = lowerDoor()
        val otherDoorDirection = otherDoorDirection(lower, hacked)

        val potentialOtherDoor = lowerBlock.getRelative(otherDoorDirection)

        val potentialOtherDoorState: Door? = asDoorState(potentialOtherDoor)

        // no iron door shenanigans.
        if (lowerBlock.type != potentialOtherDoor.type) {
            return null
        }

        // heights must match
        if (potentialOtherDoorState == null) return null
        if (potentialOtherDoorState.half != lower.half) return null

        // Door states must match, or else the door shouldn't be flapped.
        // This works for hacked doors, and normal doors, but not franken-doors (half-half)
        if (potentialOtherDoorState.isOpen != lower.isOpen) return null

        // Another door must agree that our door is its partner!
        val otherPointing = otherDoorDirection(potentialOtherDoorState, hacked)

        val shouldBeUs = potentialOtherDoor.getRelative(otherPointing)

        val isUs =
            shouldBeUs.x == lowerBlock.x &&
                shouldBeUs.y == lowerBlock.y &&
                shouldBeUs.z == lowerBlock.z

        return if (isUs) createDoorFromBlock(potentialOtherDoor) else null
    }

    /** Computes the direction where a potential partner door must be located. */
    private fun otherDoorDirection(ourDoor: Door, hacked: Boolean): BlockFace {
        // So, to find a door, we simply trace the way the door is pointing.
        // We can safely ignore opened status, since it relies upon their closed state, even for
        // hacked doors.
        val blankPartWhenClosed = ourDoor.facing
        // hacked doors always face their partner.
        if (hacked) return ourDoor.facing

        // closed doors point towards another door depending on the hinge.
        // this is still true for open doors, since the block state is based on the closed state.
        return when (ourDoor.hinge) {
            Door.Hinge.LEFT -> rotateCW(blankPartWhenClosed)
            Door.Hinge.RIGHT -> rotateCCW(blankPartWhenClosed)
        }
    }

    /** Reads or updates the open state of this door's lower block. */
    var isOpen: Boolean
        get() = lowerDoor().isOpen
        set(open) {
            val data: Door = asDoorState(lowerBlock) ?: return
            data.isOpen = open
            lowerBlock.setBlockData(data)
        }

    companion object {
        /**
         * Factory method for creating SingleDoors. Validates that the block is a door, and has a
         * 2nd half. Accepts either top or bottom blocks, generally from an interaction event. Will
         * fail and return null if block is not a door, or not a complete door.
         *
         * @param originBlock any block to create a SingleDoor instance from.
         * @return the SingleDoor instance representing the door structure.
         */
        @JvmStatic
        fun createDoorFromBlock(originBlock: Block): SingleDoor? {
            // if half is null, a door was not valid.
            if (!validateSingleDoor(originBlock)) {
                return null
            }
            return SingleDoor(getLower(originBlock))
        }

        /** Resolves the lower block from either half of a door pair. */
        private fun getLower(originBlock: Block): Block {
            val half: Bisected.Half = requireNotNull(asDoorState(originBlock)).half
            return when (half) {
                Bisected.Half.TOP -> originBlock.getRelative(BlockFace.DOWN)
                Bisected.Half.BOTTOM -> originBlock
            }
        }

        /** Validates that the origin block forms a complete two-half door. */
        private fun validateSingleDoor(originBlock: Block): Boolean {
            // block must be door.
            if (!isDoor(originBlock)) {
                return false
            }

            val otherHalf: Block = otherVerticalHalf(originBlock)

            // another half must be door.
            if (!isDoor(otherHalf)) {
                return false
            }

            // door components must be a matching pair, e.g., top and bottom.
            return doorVerticalHalvesMatch(originBlock, otherHalf)
        }

        /** Returns whether the block data currently represents a door. */
        private fun isDoor(block: Block): Boolean {
            val blockData = block.blockData
            return blockData is Door
        }

        /** @param block Must be a door. */
        private fun otherVerticalHalf(block: Block): Block {
            val door: Door = requireNotNull(asDoorState(block))
            return when (door.half) {
                Bisected.Half.TOP -> block.getRelative(BlockFace.DOWN)
                Bisected.Half.BOTTOM -> block.getRelative(BlockFace.UP)
            }
        }

        /** Returns whether both door blocks represent opposite vertical halves. */
        private fun doorVerticalHalvesMatch(originBlock: Block, otherBlock: Block): Boolean {
            val originState: Door = requireNotNull(asDoorState(originBlock))
            val expected: Bisected.Half = getOpposite(originState.half)
            return asDoorState(otherBlock)?.half == expected
        }

        /** Returns the opposite vertical half enum value. */
        private fun getOpposite(half: Bisected.Half?): Bisected.Half {
            if (half == Bisected.Half.TOP) return Bisected.Half.BOTTOM
            else if (half == Bisected.Half.BOTTOM) return Bisected.Half.TOP
            throw IllegalArgumentException("Something has fundamentally changed with Bisected.Half")
        }

        /** Casts block data from a block instance to a door state when possible. */
        private fun asDoorState(block: Block): Door? {
            return asDoorState(block.blockData)
        }

        /** Casts generic block data to door data when possible. */
        private fun asDoorState(blockData: BlockData): Door? {
            if (blockData !is Door) return null
            return blockData
        }

        /** Rotates a cardinal face 90 degrees clockwise. */
        private fun rotateCW(face: BlockFace): BlockFace {
            return when (face) {
                BlockFace.NORTH -> BlockFace.EAST
                BlockFace.EAST -> BlockFace.SOUTH
                BlockFace.SOUTH -> BlockFace.WEST
                BlockFace.WEST -> BlockFace.NORTH
                else -> throw IllegalArgumentException("This is a door utility...")
            }
        }

        /** Rotates a cardinal face 90 degrees counter-clockwise. */
        private fun rotateCCW(face: BlockFace): BlockFace {
            return when (face) {
                BlockFace.NORTH -> BlockFace.WEST
                BlockFace.EAST -> BlockFace.NORTH
                BlockFace.SOUTH -> BlockFace.EAST
                BlockFace.WEST -> BlockFace.SOUTH
                else -> throw IllegalArgumentException("This is a door utility...")
            }
        }
    }
}
