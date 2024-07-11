package org.oddlama.vane.portals.portal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.oddlama.vane.portals.PortalConstructor;

public class PortalBoundary {

	public static enum ErrorState {
		NONE,
		NO_ORIGIN,
		MULTIPLE_ORIGINS,
		TOO_SMALL_SPAWN_X,
		TOO_SMALL_SPAWN_Y,
		TOO_SMALL_SPAWN_Z,
		TOO_LARGE_X,
		TOO_LARGE_Y,
		TOO_LARGE_Z,
		NO_PORTAL_BLOCK_ABOVE_ORIGIN,
		NOT_ENOUGH_PORTAL_BLOCKS_ABOVE_ORIGIN,
		TOO_MANY_PORTAL_AREA_BLOCKS,
		PORTAL_AREA_OBSTRUCTED,
	}

	private Set<Block> boundary_blocks = null;
	private Set<Block> portal_area_blocks = null;

	// Origin block is the root block of the portal. It determines the search point
	// for the spawn location for XY and YZ types, and the region the portal belongs to.
	private Block origin_block = null;
	private final Plane plane;
	private Location spawn = null;

	private ErrorState error_state = ErrorState.NONE;
	private int dim_x, dim_y, dim_z;

	private PortalBoundary(Plane plane) {
		this.plane = plane;
	}

	// Returns all boundary blocks (excluding origin block)
	public Set<Block> boundary_blocks() {
		return boundary_blocks;
	}

	// Returns all portal area blocks
	public Set<Block> portal_area_blocks() {
		return portal_area_blocks;
	}

	/**
	 * Returns the origin block (which is part of the portal outline but not included in boundary_blocks()).
	 * Can be null if no origin block was used but a portal shape was found
	 */
	public Block origin_block() {
		return origin_block;
	}

	public Plane plane() {
		return plane;
	}

	public Location spawn() {
		return spawn;
	}

	public ErrorState error_state() {
		return error_state;
	}

	public int dim_x() {
		return dim_x;
	}

	public int dim_y() {
		return dim_y;
	}

	public int dim_z() {
		return dim_z;
	}

	public List<Block> all_blocks() {
		final var all_blocks = new ArrayList<Block>();
		all_blocks.addAll(boundary_blocks);
		all_blocks.addAll(portal_area_blocks);
		all_blocks.add(origin_block);
		return all_blocks;
	}

	public boolean intersects_existing_portal(final PortalConstructor portal_constructor) {
		for (final var b : all_blocks()) {
			if (portal_constructor.get_module().is_portal_block(b)) {
				return true;
			}
		}
		return false;
	}

	private static void push_block_if_not_contained(
		final Block block,
		final Stack<Block> stack,
		final Set<Block> out_boundary,
		final Set<Block> out_portal_area
	) {
		if (out_boundary.contains(block) || out_portal_area.contains(block)) {
			return;
		}

		stack.push(block);
	}

	private static void push_adjacent_blocks_to_stack(
		final Block block,
		final Stack<Block> stack,
		final Set<Block> out_boundary,
		final Set<Block> out_portal_area,
		Plane plane
	) {
		switch (plane) {
			case XY:
				push_block_if_not_contained(block.getRelative(1, 0, 0), stack, out_boundary, out_portal_area);
				push_block_if_not_contained(block.getRelative(-1, 0, 0), stack, out_boundary, out_portal_area);
				push_block_if_not_contained(block.getRelative(0, 1, 0), stack, out_boundary, out_portal_area);
				push_block_if_not_contained(block.getRelative(0, -1, 0), stack, out_boundary, out_portal_area);
				break;
			case YZ:
				push_block_if_not_contained(block.getRelative(0, 0, 1), stack, out_boundary, out_portal_area);
				push_block_if_not_contained(block.getRelative(0, 0, -1), stack, out_boundary, out_portal_area);
				push_block_if_not_contained(block.getRelative(0, 1, 0), stack, out_boundary, out_portal_area);
				push_block_if_not_contained(block.getRelative(0, -1, 0), stack, out_boundary, out_portal_area);
				break;
			case XZ:
				push_block_if_not_contained(block.getRelative(1, 0, 0), stack, out_boundary, out_portal_area);
				push_block_if_not_contained(block.getRelative(-1, 0, 0), stack, out_boundary, out_portal_area);
				push_block_if_not_contained(block.getRelative(0, 0, 1), stack, out_boundary, out_portal_area);
				push_block_if_not_contained(block.getRelative(0, 0, -1), stack, out_boundary, out_portal_area);
				break;
		}
	}

	private static void do_flood_fill4_step(
		final PortalConstructor portal_constructor,
		final Stack<Block> stack,
		final Set<Block> out_boundary,
		final Set<Block> out_portal_area,
		final Plane plane
	) {
		final var block = stack.pop();
		if (portal_constructor.is_type_part_of_boundary_or_origin(block.getType())) {
			out_boundary.add(block);
		} else {
			out_portal_area.add(block);
			push_adjacent_blocks_to_stack(block, stack, out_boundary, out_portal_area, plane);
		}
	}

	/**
	 * Simultaneously fill two areas.
	 * Return as soon as a valid area is found or the maximum depth is exceeded.
	 * Returns a pair of { boundary, portal_area }
	 */
	private static Pair<Set<Block>, Set<Block>> simultaneous_flood_fill4(
		final PortalConstructor portal_constructor,
		final Block[] areas,
		final Plane plane
	) {
		final var boundary0 = new HashSet<Block>();
		final var boundary1 = new HashSet<Block>();
		final var portal_area0 = new HashSet<Block>();
		final var portal_area1 = new HashSet<Block>();
		final var flood_fill_stack0 = new Stack<Block>();
		final var flood_fill_stack1 = new Stack<Block>();

		if (areas[0] != null) {
			flood_fill_stack0.push(areas[0]);
		}
		if (areas[1] != null) {
			flood_fill_stack1.push(areas[1]);
		}

		// Keep going as long as all stacks of enabled areas are not empty and max depth is not reached
		int depth = 0;
		while (
			(areas[0] == null || !flood_fill_stack0.isEmpty()) && (areas[1] == null || !flood_fill_stack1.isEmpty())
		) {
			++depth;

			// Maximum depth reached -> both areas are invalid
			if (depth > portal_constructor.config_area_floodfill_max_steps) {
				return null;
			}

			if (areas[0] != null) {
				do_flood_fill4_step(portal_constructor, flood_fill_stack0, boundary0, portal_area0, plane);
			}
			if (areas[1] != null) {
				do_flood_fill4_step(portal_constructor, flood_fill_stack1, boundary1, portal_area1, plane);
			}
		}

		if (areas[0] != null && flood_fill_stack0.isEmpty()) {
			return Pair.of(boundary0, portal_area0);
		} else if (areas[1] != null && flood_fill_stack1.isEmpty()) {
			return Pair.of(boundary1, portal_area1);
		}

		// Cannot occur.
		return null;
	}

	private static List<Block> get_surrounding_blocks_ccw(final Block block, final Plane plane) {
		final var surrounding_blocks = new ArrayList<Block>();

		switch (plane) {
			case XY:
				surrounding_blocks.add(block.getRelative(1, -1, 0));
				surrounding_blocks.add(block.getRelative(1, 0, 0));
				surrounding_blocks.add(block.getRelative(1, 1, 0));
				surrounding_blocks.add(block.getRelative(0, 1, 0));
				surrounding_blocks.add(block.getRelative(-1, 1, 0));
				surrounding_blocks.add(block.getRelative(-1, 0, 0));
				surrounding_blocks.add(block.getRelative(-1, -1, 0));
				surrounding_blocks.add(block.getRelative(0, -1, 0));
				break;
			case YZ:
				surrounding_blocks.add(block.getRelative(0, 1, -1));
				surrounding_blocks.add(block.getRelative(0, 1, 0));
				surrounding_blocks.add(block.getRelative(0, 1, 1));
				surrounding_blocks.add(block.getRelative(0, 0, 1));
				surrounding_blocks.add(block.getRelative(0, -1, 1));
				surrounding_blocks.add(block.getRelative(0, -1, 0));
				surrounding_blocks.add(block.getRelative(0, -1, -1));
				surrounding_blocks.add(block.getRelative(0, 0, -1));
				break;
			case XZ:
				surrounding_blocks.add(block.getRelative(1, 0, -1));
				surrounding_blocks.add(block.getRelative(1, 0, 0));
				surrounding_blocks.add(block.getRelative(1, 0, 1));
				surrounding_blocks.add(block.getRelative(0, 0, 1));
				surrounding_blocks.add(block.getRelative(-1, 0, 1));
				surrounding_blocks.add(block.getRelative(-1, 0, 0));
				surrounding_blocks.add(block.getRelative(-1, 0, -1));
				surrounding_blocks.add(block.getRelative(0, 0, -1));
				break;
		}

		return surrounding_blocks;
	}

	private static Block[] get_potential_area_blocks(
		final PortalConstructor portal_constructor,
		final Block block,
		final Plane plane
	) {
		/* Step 1: Assert that the 8 surrounding blocks must include two or more boundary blocks
		 * Step 2: Set area index to first area
		 * Step 3: Start at any surrounding block.
		 * Step 4: Check if the block is a boundary block
		 *  - false: Set this as the start block for the current area.
		 *           If both areas are assigned, stop here.
		 *  - true: Set the area index to the other area (area + 1) % 2 if it is the first boundary block after a non boundary block
		 * Step 5: Select next block CW/CCW. If it is the start block return the areas, else to step 4
		 */

		final var surrounding_blocks = get_surrounding_blocks_ccw(block, plane);

		// Assert that there are exactly two boundary blocks
		int boundary_blocks = 0;
		for (final var surrounding_block : surrounding_blocks) {
			if (portal_constructor.is_type_part_of_boundary_or_origin(surrounding_block.getType())) {
				++boundary_blocks;
			}
		}

		if (boundary_blocks < 2) {
			return null;
		}

		// Identify areas
		final var areas = new Block[2];
		int area_index = 0;
		boolean had_boundary_block_before = false;
		for (final var surrounding_block : surrounding_blocks) {
			// Examine a block type
			if (portal_constructor.is_type_part_of_boundary_or_origin(surrounding_block.getType())) {
				if (!had_boundary_block_before) area_index = (area_index + 1) % 2;

				had_boundary_block_before = true;
			} else {
				areas[area_index] = surrounding_block;

				// Check if another area is also set
				if (areas[(area_index + 1) % 2] != null) return areas;

				had_boundary_block_before = false;
			}
		}

		// Only less than two areas were found.
		return areas;
	}

	private static void add3_air_stacks(
		final PortalConstructor portal_constructor,
		final Block start_air,
		final List<Block> lowest_air_blocks,
		boolean insert_front,
		int mod_x,
		int mod_z
	) {
		var air = start_air;
		while (true) {
			air = air.getRelative(-mod_x, 0, -mod_z);
			if (air.getType() != portal_constructor.config_material_portal_area) {
				break;
			}

			final var boundary = air.getRelative(0, -1, 0);
			if (portal_constructor.is_type_part_of_boundary(boundary.getType())) {
				break;
			}

			final var above1 = air.getRelative(0, 1, 0);
			if (above1.getType() != portal_constructor.config_material_portal_area) {
				break;
			}

			final var above2 = air.getRelative(0, 2, 0);
			if (above2.getType() != portal_constructor.config_material_portal_area) {
				break;
			}

			if (insert_front) {
				lowest_air_blocks.add(0, air);
			} else {
				lowest_air_blocks.add(air);
			}
		}
	}

	private static PortalBoundary search_at(
		final PortalConstructor portal_constructor,
		final Block search_block,
		Plane plane
	) {
		/* A 3x3 field of blocks around the start block is always split into 2 areas:
		 *
		 * []''''
		 * ..##[]
		 * ......
		 *
		 * [] = boundary
		 * ## = start boundary block
		 * .. = area 1
		 * '' = area 2
		 *
		 * Anything else is invalid.
		 *
		 * Step 1: Determine one block each, for area 1 and area 2
		 * Step 2: Do a flood fill algorithm at the same time on both areas
		 *  - The one that finishes first wins
		 *  - If both exceed (two times) the max block count, it is invalid
		 * Result: The flood fill algorithm returns the boundary and portal area for the valid area or null if both are invalid.
		 */

		final var potential_area_blocks = get_potential_area_blocks(portal_constructor, search_block, plane);

		// If potential_area_blocks is null, the shape is invalid
		if (potential_area_blocks == null || (potential_area_blocks[0] == null && potential_area_blocks[1] == null)) {
			return null;
		}

		final var result = simultaneous_flood_fill4(portal_constructor, potential_area_blocks, plane);
		if (result == null) {
			return null;
		}

		final var boundary = new PortalBoundary(plane);
		boundary.boundary_blocks = result.getLeft();
		boundary.portal_area_blocks = result.getRight();

		// Remove origin block from a boundary list
		final var iterator = boundary.boundary_blocks.iterator();
		while (iterator.hasNext()) {
			final var block = iterator.next();
			if (block.getType() == portal_constructor.config_material_origin) {
				if (boundary.origin_block != null) {
					// Duplicate origin block
					boundary.error_state = ErrorState.MULTIPLE_ORIGINS;
					return boundary;
				} else {
					iterator.remove();
					boundary.origin_block = block;
				}
			}
		}

		// Check origin existence
		if (boundary.origin_block == null) {
			boundary.error_state = ErrorState.NO_ORIGIN;
			return boundary;
		}

		// Check area size
		if (boundary.portal_area_blocks.size() > portal_constructor.config_area_max_blocks) {
			boundary.error_state = ErrorState.TOO_MANY_PORTAL_AREA_BLOCKS;
			return boundary;
		}

		// Check maximum size constraints
		int min_x = Integer.MAX_VALUE, min_y = Integer.MAX_VALUE, min_z = Integer.MAX_VALUE;
		int max_x = Integer.MIN_VALUE, max_y = Integer.MIN_VALUE, max_z = Integer.MIN_VALUE;

		for (final var block : boundary.portal_area_blocks) {
			min_x = Math.min(min_x, block.getX());
			min_y = Math.min(min_y, block.getY());
			min_z = Math.min(min_z, block.getZ());
			max_x = Math.max(max_x, block.getX());
			max_y = Math.max(max_y, block.getY());
			max_z = Math.max(max_z, block.getZ());
		}

		boundary.dim_x = 1 + max_x - min_x;
		boundary.dim_y = 1 + max_y - min_y;
		boundary.dim_z = 1 + max_z - min_z;

		if (boundary.dim_x > portal_constructor.max_dim_x(plane)) {
			boundary.error_state = ErrorState.TOO_LARGE_X;
			return boundary;
		} else if (boundary.dim_y > portal_constructor.max_dim_y(plane)) {
			boundary.error_state = ErrorState.TOO_LARGE_Y;
			return boundary;
		} else if (boundary.dim_z > portal_constructor.max_dim_z(plane)) {
			boundary.error_state = ErrorState.TOO_LARGE_Z;
			return boundary;
		}

		Set<Material> air_overrides = Set.of(Material.CAVE_AIR, Material.AIR, Material.VOID_AIR);

		// Check area obstruction
		for (final var block : boundary.portal_area_blocks) {
			if (block.getType() != portal_constructor.config_material_portal_area) {
				if (portal_constructor.config_material_portal_area == Material.AIR) {
					if (air_overrides.contains(block.getType())) {
						continue;
					}
				}
				boundary.error_state = ErrorState.PORTAL_AREA_OBSTRUCTED;
				return boundary;
			}
		}

		// Determine spawn point and check minimum size constraints (these are only important at the portal's spawn point)
		if (boundary.plane == Plane.XZ) {
			// Find middle of portal, then find first (2,3x2,3) (even,uneven) area in the direction of origin block (the greater x/z distance is chosen)
			final var middle_small_coords = boundary
				.origin_block()
				.getWorld()
				.getBlockAt(
					(int) Math.floor(min_x / 2.0 + max_x / 2.0),
					boundary.origin_block().getY(),
					(int) Math.floor(min_z / 2.0 + max_z / 2.0)
				);

			int diff_x = middle_small_coords.getX() - boundary.origin_block().getX();
			int diff_z = middle_small_coords.getZ() - boundary.origin_block().getZ();

			// Calculate mod to add to middle block to get more into the direction of origin block
			int mod_x, mod_z;
			int abs_mod_x, abs_mod_z;
			if (Math.abs(diff_x) > Math.abs(diff_z)) {
				mod_x = diff_x > 0 ? -1 : 1;
				abs_mod_x = 1;
				mod_z = abs_mod_z = 0;
			} else {
				mod_x = abs_mod_x = 0;
				abs_mod_z = 1;
				mod_z = diff_z > 0 ? -1 : 1;
			}

			// Find a strip of portal blocks last along the axis defined by the origin block.
			// If there are less than 2 blocks inside, the spawn area is too small.
			// Therefore, walk to origin until a block inside the area is found.
			var first_inside = middle_small_coords;
			while (!boundary.portal_area_blocks.contains(first_inside)) {
				if (mod_x != 0) {
					if (first_inside.getX() <= min_x || first_inside.getX() >= max_x) {
						boundary.error_state = ErrorState.TOO_SMALL_SPAWN_X;
						return boundary;
					}
				}

				if (mod_z != 0) {
					if (first_inside.getZ() <= min_z || first_inside.getZ() >= max_z) {
						boundary.error_state = ErrorState.TOO_SMALL_SPAWN_Z;
						return boundary;
					}
				}

				first_inside = first_inside.getRelative(mod_x, 0, mod_z);
			}

			// Find "backwards" last block inside
			Block next = first_inside;
			Block back_last_inside;
			int total_blocks_inside = -1; // -1 because both forward and backward search includes first_inside
			do {
				back_last_inside = next;
				next = back_last_inside.getRelative(-mod_x, 0, -mod_z);
				++total_blocks_inside;
			} while (boundary.portal_area_blocks().contains(next));

			// Find "forward" last block inside
			next = first_inside;
			Block last_inside;
			do {
				last_inside = next;
				next = last_inside.getRelative(mod_x, 0, mod_z);
				++total_blocks_inside;
			} while (boundary.portal_area_blocks().contains(next));

			if (total_blocks_inside < 2) {
				boundary.error_state = mod_z == 0 ? ErrorState.TOO_SMALL_SPAWN_X : ErrorState.TOO_SMALL_SPAWN_Z;
				return boundary;
			}

			// Get block in the middle (if block edge round to smaller coords)
			int m_x, m_z;
			if (mod_x == 0) {
				m_x = (int) Math.floor(min_x / 2.0 + max_x / 2.0);
				m_z = (int) Math.floor(last_inside.getZ() / 2.0 + back_last_inside.getZ() / 2.0);
			} else {
				m_x = (int) Math.floor(last_inside.getX() / 2.0 + back_last_inside.getX() / 2.0);
				m_z = (int) Math.floor(min_z / 2.0 + max_z / 2.0);
			}

			final var middle_inside = last_inside.getWorld().getBlockAt(m_x, last_inside.getY(), m_z);

			// The origin axis will have its evenness determined by the number of connected air blocks
			boolean even_along_origin_axis = total_blocks_inside % 2 == 0;
			boolean even_along_side_axis = (mod_x == 0 ? boundary.dim_z : boundary.dim_x) % 2 == 0;

			double spawn_offset_along_origin_axis;
			double spawn_offset_along_side_axis;
			if (even_along_origin_axis) {
				// Include coords of a middle plus one along origin direction
				if (!boundary.portal_area_blocks().contains(middle_inside.getRelative(abs_mod_x, 0, abs_mod_z))) {
					boundary.error_state = mod_x == 0 ? ErrorState.TOO_SMALL_SPAWN_Z : ErrorState.TOO_SMALL_SPAWN_X;
					return boundary;
				}

				spawn_offset_along_origin_axis = 1.0;
			} else {
				// Include coords of middle plus one and minus one along origin direction
				if (!boundary.portal_area_blocks().contains(middle_inside.getRelative(abs_mod_x, 0, abs_mod_z))) {
					boundary.error_state = mod_x == 0 ? ErrorState.TOO_SMALL_SPAWN_Z : ErrorState.TOO_SMALL_SPAWN_X;
					return boundary;
				}

				if (!boundary.portal_area_blocks().contains(middle_inside.getRelative(-abs_mod_x, 0, -abs_mod_z))) {
					boundary.error_state = mod_x == 0 ? ErrorState.TOO_SMALL_SPAWN_Z : ErrorState.TOO_SMALL_SPAWN_X;
					return boundary;
				}

				spawn_offset_along_origin_axis = 0.5;
			}

			if (even_along_side_axis) {
				// Include coords of a middle plus one along a side direction
				if (!boundary.portal_area_blocks().contains(middle_inside.getRelative(abs_mod_z, 0, abs_mod_x))) {
					boundary.error_state = mod_x == 0 ? ErrorState.TOO_SMALL_SPAWN_X : ErrorState.TOO_SMALL_SPAWN_Z;
					return boundary;
				}

				spawn_offset_along_side_axis = 1.0;
			} else {
				// Include coords of middle plus and minus one along a side direction
				if (!boundary.portal_area_blocks().contains(middle_inside.getRelative(abs_mod_z, 0, abs_mod_x))) {
					boundary.error_state = mod_x == 0 ? ErrorState.TOO_SMALL_SPAWN_X : ErrorState.TOO_SMALL_SPAWN_Z;
					return boundary;
				}

				if (!boundary.portal_area_blocks().contains(middle_inside.getRelative(-abs_mod_z, 0, -abs_mod_x))) {
					boundary.error_state = mod_x == 0 ? ErrorState.TOO_SMALL_SPAWN_X : ErrorState.TOO_SMALL_SPAWN_Z;
					return boundary;
				}

				spawn_offset_along_side_axis = 0.5;
			}

			double spawn_x, spawn_z;
			if (mod_x == 0) {
				spawn_x = m_x + spawn_offset_along_side_axis;
				spawn_z = m_z + spawn_offset_along_origin_axis;
			} else {
				spawn_x = m_x + spawn_offset_along_origin_axis;
				spawn_z = m_z + spawn_offset_along_side_axis;
			}

			boundary.spawn = new Location(middle_inside.getWorld(), spawn_x, middle_inside.getY() + 0.5, spawn_z);
		} else {
			// Find the air (above) boundary (below) combinations at origin block, determine middle, check if minimum size rectangle is part of the blocklist

			// The block above the origin block must be part of the portal
			final var air_above_origin = boundary.origin_block().getRelative(0, 1, 0);
			if (!boundary.portal_area_blocks().contains(air_above_origin)) {
				boundary.error_state = ErrorState.NO_PORTAL_BLOCK_ABOVE_ORIGIN;
				return boundary;
			}

			final var air_above_with_boundary_below = new ArrayList<Block>();
			air_above_with_boundary_below.add(air_above_origin);

			// Check for at least 1x3 air blocks above the origin
			final var air_above_origin2 = boundary.origin_block().getRelative(0, 2, 0);
			final var air_above_origin3 = boundary.origin_block().getRelative(0, 3, 0);
			if (
				!boundary.portal_area_blocks().contains(air_above_origin2) ||
				!boundary.portal_area_blocks().contains(air_above_origin3)
			) {
				boundary.error_state = ErrorState.NOT_ENOUGH_PORTAL_BLOCKS_ABOVE_ORIGIN;
				return boundary;
			}

			int mod_x = boundary.plane().x() ? 1 : 0;
			int mod_z = boundary.plane().z() ? 1 : 0;

			// Find matching air stacks to negative axis side
			add3_air_stacks(portal_constructor, air_above_origin, air_above_with_boundary_below, false, -mod_x, -mod_z);

			// Find matching pairs to positive axis side
			add3_air_stacks(portal_constructor, air_above_origin, air_above_with_boundary_below, true, mod_x, mod_z);

			// Must be at least 1x3 area of portal blocks to be valid
			if (air_above_with_boundary_below.size() < 1) {
				boundary.error_state =
					boundary.plane().x() ? ErrorState.TOO_SMALL_SPAWN_X : ErrorState.TOO_SMALL_SPAWN_Z;
				return boundary;
			}

			// Spawn location is middle of air blocks
			final var small_coord_end = air_above_with_boundary_below.get(0);
			final var large_coord_end = air_above_with_boundary_below.get(air_above_with_boundary_below.size() - 1);
			final var middle_x = 0.5 + (small_coord_end.getX() + large_coord_end.getX()) / 2.0;
			final var middle_z = 0.5 + (small_coord_end.getZ() + large_coord_end.getZ()) / 2.0;
			boundary.spawn =
				new Location(air_above_origin.getWorld(), middle_x, air_above_origin.getY() + 0.05, middle_z);
		}

		return boundary;
	}

	public static PortalBoundary search_at(final PortalConstructor portal_constructor, final Block block) {
		var boundary = search_at(portal_constructor, block, Plane.XY);
		if (boundary != null) {
			return boundary;
		}

		boundary = search_at(portal_constructor, block, Plane.YZ);
		if (boundary != null) {
			return boundary;
		}

		return search_at(portal_constructor, block, Plane.XZ);
	}

	@Override
	public String toString() {
		return "PortalBoundary{origin_block = " + origin_block + ", plane = " + plane + "}";
	}
}
