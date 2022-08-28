package org.oddlama.vane.trifles;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract short-lived container to manage door states.
 * All state is assumed from the bottom half, relies on Minecraft to restore bad block states.
 */
public class SingleDoor {

	private final Block lower_block; // Must be lower block
	private Door lower;
	private Door upper;

	/**
	 * Assumes valid door location already.
	 * @param lower_block Block of the lower part of the door.
	 */
	private SingleDoor(Block lower_block) {
		this.lower_block = lower_block;
		this.lower = as_door_state(lower_block);
		this.upper = as_door_state(lower_block.getRelative(BlockFace.UP));
	}

	/**
	 * Factory method for creating SingleDoors. validates that the block is a door, and has a 2nd half.
	 * Accepts either top or bottom blocks, generally from an interaction event.
	 * Will fail and return null if block is not a door, or not a complete door.
	 * @param originBlock any block to create a SingleDoor instance from.
	 * @return the SingleDoor instance representing the door structure.
	 */
	@Nullable
	public static SingleDoor create_door_from_block(final Block originBlock) {
		// if half is null, door was not valid.
		if (!validate_single_door(originBlock)) {
			return null;
		}
		return new SingleDoor(get_lower(originBlock));
	}

	private static Block get_lower(Block originBlock) {
		final var half = as_door_state(originBlock).getHalf();
		return switch (half) {
			case TOP -> originBlock.getRelative(BlockFace.DOWN);
			case BOTTOM -> originBlock;
		};
	}

	/**
	 * Validates the door structure, and returns the half of the door we are examining.
	 */
	private static boolean validate_single_door(final Block originBlock) {
		// block must be door.
		if (!is_door(originBlock)) {
			return false;
		}

		final var other_half = other_vertical_half(originBlock);

		// other half must be door.
		if (!is_door(other_half)) {
			return false;
		}

		// door components must be a matching pair, e.g. top and bottom.
		return door_vertical_halves_match(originBlock, other_half);
	}

	private static boolean is_door(final Block block) {
		final var block_data = block.getBlockData();
		return block_data instanceof Door;
	}

	/**
	 * @param block Must be a door.
	 */
	private static Block other_vertical_half(Block block) {
		final Door door = as_door_state(block);
		return switch (door.getHalf()) {
			case TOP -> block.getRelative(BlockFace.DOWN);
			case BOTTOM -> block.getRelative(BlockFace.UP);
		};
	}

	private static boolean door_vertical_halves_match(Block origin_block, Block other_block) {
		final Door originState = as_door_state(origin_block);
		final var expected = get_opposite(originState.getHalf());
		return as_door_state(other_block).getHalf() == expected;
	}

	private static Bisected.Half get_opposite(Bisected.Half half) {
		if (half == Bisected.Half.TOP) return Bisected.Half.BOTTOM; else if (
			half == Bisected.Half.BOTTOM
		) return Bisected.Half.TOP;
		throw new IllegalArgumentException("Something has fundamentally changed with Bisected.Half");
	}

	private static Door as_door_state(Block block) {
		return as_door_state(block.getBlockData());
	}

	private static Door as_door_state(BlockData block_data) {
		if (!(block_data instanceof Door)) return null;
		return (Door) block_data;
	}

	public boolean update_cached_state() {
		// Update to the current state if possible
		final var lower_data = lower_block.getBlockData();
		lower = as_door_state(lower_data);
		final var upper_data = other_vertical_half(lower_block).getBlockData();
		upper = as_door_state(upper_data);

		if (lower == null) return false;
		return upper != null;
	}

	public void set_open(boolean open) {
		var data = as_door_state(lower_block);
		data.setOpen(open);
		lower_block.setBlockData(data);
	}

	/**
	 * Gets the SingleDoor instance representing the other half of this double door.
	 * @return null, if no matching door was found.
	 */
	public SingleDoor get_second_door() {
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
		// the following is a picture of a door block from above. (built by a player facing south)
		// OXXXXX // X = door hitbox
		// |    | // o = door hinge.
		// |    | // (the rest is empty)
		// ------
		// in this instance, the door is facing 'south' with the hinge in the NW corner.
		//
		// It's hinge is 'right' as it is facing south.
		// It is currently 'closed'.
		//
		// The 'open' door is strange. it retains all properties, except it's now open.
		//
		// O----- // X = door hitbox
		// X    | // o = door hinge.
		// X    | // (the rest is empty)
		// X-----
		// in this instance, the door is *STILL* facing 'south' with the hinge in the NW corner.
		//
		// It's hinge is *still* 'right' and it is defined as facing south, even though it has rotated.
		//
		// # Double Doors (built by a player facing north)
		//        1      2
		// WWWWWW ------ ------ WWWWWW // X = door hitbox
		// WWWWWW |    | |    | WWWWWW // O = door hinge.
		// WWWWWW |    | |    | WWWWWW // W = Solid block
		// WWWWWW OXXXXX XXXXXO WWWWWW // (the rest is empty)
		// Closed.
		//
		// Door 1 is minecraft:acacia_door[facing=north,half=lower,hinge=left,open=false,powered=false]
		// Door 2 is minecraft:acacia_door[facing=north,half=lower,hinge=right,open=false,powered=false]
		//
		//        1      2
		// WWWWWW X----- -----X WWWWWW // X = door hitbox
		// WWWWWW X    | |    X WWWWWW // O = door hinge.
		// WWWWWW X    | |    X WWWWWW // W = Solid block
		// WWWWWW O----- -----O WWWWWW // (the rest is empty)
		// Open.
		//
		// Door 1 is minecraft:acacia_door[facing=north,half=lower,hinge=left,open=true,powered=false]
		// Door 2 is minecraft:acacia_door[facing=north,half=lower,hinge=right,open=true,powered=false]
		//
		// # Double Doors "Hacky Tricky Type"
		//
		// Players often employ a trick when constructing doors.
		// Mobs will only navigate through open doors and will treat closed doors as unpassable.
		// It is possible to construct doors that confuse mobs, (and us) to have doors that mob's won't break.
		// or ever pass through.
		//
		// ## 'closed' (but open to the player).
		//        1      2
		// WWWWWW X----- -----X WWWWWW // X = door hitbox
		// WWWWWW X    | |    X WWWWWW // O = door hinge.
		// WWWWWW X    | |    X WWWWWW // W = Solid block
		// WWWWWW O----- -----O WWWWWW // (the rest is empty)
		// Door 1:
		// /setblock 0 100 -3 minecraft:acacia_door[facing=east,half=lower,hinge=right,open=true,powered=false]
		// Door 2:
		// /setblock 1 100 -3 minecraft:acacia_door[facing=west,half=lower,hinge=left,open=true,powered=false]
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
		// /setblock 0 100 -3 minecraft:acacia_door[facing=east,half=lower,hinge=right,open=false,powered=false]
		// Door 2:
		// /setblock 1 100 -3 minecraft:acacia_door[facing=west,half=lower,hinge=left,open=false,powered=false]

		// Because of this, it is possible to construct a set of double doors, and a door can simultaneously
		// belong to 2 different door sets, a 'hacked' door-set, and a normal door-set.
		// trying to account for this would end up with a cellular automata of updates, so instead we prioritize
		// opening over closing.

		var normal_door = find_other_door(false);
		var hacked_door = find_other_door(true);

		// User testing showed that doors 'looked weird' unless you prioritized the doors that 'connect' in a case of
		// conflict
		return priortize(normal_door, hacked_door);
	}

	private SingleDoor priortize(SingleDoor normal_door, SingleDoor hacked_door) {
		if (normal_door == null) return hacked_door;
		if (hacked_door == null) return normal_door;
		if (lower.isOpen()) return hacked_door;
		return normal_door;
	}

	@org.jetbrains.annotations.Nullable
	private SingleDoor find_other_door(boolean hacked) {
		var otherDoorDirection = this.other_door_direction(lower, hacked);

		final Block potentialOtherDoor = lower_block.getRelative(otherDoorDirection);

		final Door potential_other_door_state = as_door_state(potentialOtherDoor);
		if (potential_other_door_state == null) return null;

		// no iron door shenanigans.
		if (lower_block.getType() != potentialOtherDoor.getType()) {
			return null;
		}

		// heights must match
		if (potential_other_door_state.getHalf() != lower.getHalf()) return null;

		// door states must match, or else the door shouldn't be flapped.
		// This works for hacked doors, and normal doors, but not franken-doors (half-half)
		if (potential_other_door_state.isOpen() != lower.isOpen()) return null;

		// Other door must agree that our door is its partner!
		final var other_pointing = other_door_direction(potential_other_door_state, hacked);

		var should_be_us = potentialOtherDoor.getRelative(other_pointing);

		var is_us =
			should_be_us.getX() == lower_block.getX() &&
			should_be_us.getY() == lower_block.getY() &&
			should_be_us.getZ() == lower_block.getZ();

		return is_us ? create_door_from_block(potentialOtherDoor) : null;
	}

	private BlockFace other_door_direction(Door our_door, boolean hacked) {
		// So, in order to find a door, we simply trace the way the door is pointing.
		// We can safely ignore opened status, since it relies upon their closed state, even for hacked doors.
		final var blank_part_when_closed = our_door.getFacing();
		// hacked doors always face their partner.
		if (hacked) return our_door.getFacing();

		// closed doors point towards other door depending on the hinge.
		// this is still true for open doors, since the blockstate is based on the closed state.
		return switch (our_door.getHinge()) {
			case LEFT -> rotateCW(blank_part_when_closed);
			case RIGHT -> rotateCCW(blank_part_when_closed);
		};
	}

	private static BlockFace rotateCW(BlockFace face) {
		return switch (face) {
			case NORTH -> BlockFace.EAST;
			case EAST -> BlockFace.SOUTH;
			case SOUTH -> BlockFace.WEST;
			case WEST -> BlockFace.NORTH;
			default -> throw new IllegalArgumentException("This is a door utility...");
		};
	}

	private static BlockFace rotateCCW(BlockFace face) {
		return switch (face) {
			case NORTH -> BlockFace.WEST;
			case EAST -> BlockFace.NORTH;
			case SOUTH -> BlockFace.EAST;
			case WEST -> BlockFace.SOUTH;
			default -> throw new IllegalArgumentException("This is a door utility...");
		};
	}

	public boolean isOpen() {
		return lower.isOpen();
	}
}
