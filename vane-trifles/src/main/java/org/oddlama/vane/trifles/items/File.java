package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.BlockUtil.raytrace_dominant_face;
import static org.oddlama.vane.util.BlockUtil.raytrace_oct;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.BlockUtil;

import net.kyori.adventure.key.Key;

@VaneItem(name = "file", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 4000, model_data = 0x760003, version = 1)
public class File extends CustomItem<Trifles> {
	public File(Context<Trifles> context) {
		super(context);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape(" m", "s ")
			.set_ingredient('m', Material.IRON_INGOT)
			.set_ingredient('s', Material.STICK)
			.result(key().toString()));
	}

	@Override
	public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.TEMPT, InhibitBehavior.USE_OFFHAND);
	}

	private BlockFace next_facing(Set<BlockFace> allowed_faces, BlockFace face) {
		if (allowed_faces.isEmpty()) {
			return face;
		}
		final var list = new ArrayList<BlockFace>(allowed_faces);
		Collections.sort(list, (a, b) -> a.ordinal() - b.ordinal());
		final var index = list.indexOf(face);
		if (index == -1) {
			return face;
		}
		return list.get((index + 1) % list.size());
	}

	private Sound change_stair_half(final Stairs stairs) {
		// Change half
		stairs.setHalf(stairs.getHalf() == Bisected.Half.BOTTOM ? Bisected.Half.TOP : Bisected.Half.BOTTOM);
		return Sound.UI_STONECUTTER_TAKE_RESULT;
	}

	private BlockFace next_face_ccw(final BlockFace face) {
		switch (face) {
			default:
				return null;
			case NORTH:
				return BlockFace.WEST;
			case EAST:
				return BlockFace.NORTH;
			case SOUTH:
				return BlockFace.EAST;
			case WEST:
				return BlockFace.SOUTH;
		}
	}

	private Sound change_stair_shape(
		final Player player,
		final Block block,
		final Stairs stairs,
		final BlockFace clicked_face
	) {
		// Check which eighth of the block was clicked
		final var oct = raytrace_oct(player, block);
		if (oct == null) {
			return null;
		}

		var corner = oct.corner();
		final var is_stair_top = stairs.getHalf() == Bisected.Half.TOP;

		// If we clicked on the base part, toggle the corner upstate,
		// as we always want to reason abount the corner to add/remove.
		if (corner.up() == is_stair_top) {
			corner = corner.up(!corner.up());
		}

		// Rotate a corner so that we can interpret it from
		// the reference facing (north)
		final var original_facing = stairs.getFacing();
		corner = corner.rotate_to_north_reference(original_facing);

		// Determine the resulting shape, face and wether the oct got added or removed
		Stairs.Shape shape = null;
		BlockFace face = null;
		boolean added = false;
		switch (stairs.getShape()) {
			case STRAIGHT:
				switch (corner.xz_face()) {
					case SOUTH_WEST:
						shape = Stairs.Shape.INNER_LEFT;
						face = BlockFace.NORTH;
						added = true;
						break;
					case NORTH_WEST:
						shape = Stairs.Shape.OUTER_RIGHT;
						face = BlockFace.NORTH;
						added = false;
						break;
					case NORTH_EAST:
						shape = Stairs.Shape.OUTER_LEFT;
						face = BlockFace.NORTH;
						added = false;
						break;
					case SOUTH_EAST:
						shape = Stairs.Shape.INNER_RIGHT;
						face = BlockFace.NORTH;
						added = true;
						break;
					default:
						break;
				}
				break;
			case INNER_LEFT:
				switch (corner.xz_face()) {
					case SOUTH_WEST:
						shape = Stairs.Shape.STRAIGHT;
						face = BlockFace.NORTH;
						added = false;
						break;
					case NORTH_EAST:
						shape = Stairs.Shape.STRAIGHT;
						face = BlockFace.WEST;
						added = false;
						break;
					default:
						break;
				}
				break;
			case INNER_RIGHT:
				switch (corner.xz_face()) {
					case NORTH_WEST:
						shape = Stairs.Shape.STRAIGHT;
						face = BlockFace.EAST;
						added = false;
						break;
					case SOUTH_EAST:
						shape = Stairs.Shape.STRAIGHT;
						face = BlockFace.NORTH;
						added = false;
						break;
					default:
						break;
				}
				break;
			case OUTER_LEFT:
				switch (corner.xz_face()) {
					case SOUTH_WEST:
						shape = Stairs.Shape.STRAIGHT;
						face = BlockFace.WEST;
						added = true;
						break;
					case NORTH_EAST:
						shape = Stairs.Shape.STRAIGHT;
						face = BlockFace.NORTH;
						added = true;
						break;
					default:
						break;
				}
				break;
			case OUTER_RIGHT:
				switch (corner.xz_face()) {
					case NORTH_WEST:
						shape = Stairs.Shape.STRAIGHT;
						face = BlockFace.NORTH;
						added = true;
						break;
					case SOUTH_EAST:
						shape = Stairs.Shape.STRAIGHT;
						face = BlockFace.EAST;
						added = true;
						break;
					default:
						break;
				}
				break;
		}

		// Break if the resulting shape is invalid
		if (shape == null) {
			return null;
		}

		// Undo reference rotation
		switch (face) {
			case NORTH:
				face = original_facing;
				break;
			case EAST:
				face = next_face_ccw(original_facing).getOppositeFace();
				break;
			case SOUTH:
				face = original_facing.getOppositeFace();
				break;
			case WEST:
				face = next_face_ccw(original_facing);
				break;
			default:
				break;
		}

		stairs.setShape(shape);
		stairs.setFacing(face);

		return added ? Sound.UI_STONECUTTER_TAKE_RESULT : Sound.BLOCK_GRINDSTONE_USE;
	}

	private Sound change_multiple_facing(
		final Player player,
		final Block block,
		final MultipleFacing mf,
		BlockFace clicked_face
	) {
		final int min_faces;
		if (mf instanceof Fence || mf instanceof GlassPane) {
			// Allow fences and glass panes to have 0 faces
			min_faces = 0;

			// Trace which side is the dominant side
			final var result = raytrace_dominant_face(player, block);
			if (result == null) {
				return null;
			}

			// Only replace facing choice if we did hit a side,
			// or if the dominance was big enough.
			if (result.dominance > .2 || (clicked_face != BlockFace.UP && clicked_face != BlockFace.DOWN)) {
				clicked_face = result.face;
			}
		} else if (mf instanceof Tripwire) {
			min_faces = 0;
		} else {
			return null;
		}

		// Check if the clicked face is allowed to change
		if (!mf.getAllowedFaces().contains(clicked_face)) {
			return null;
		}

		boolean has_face = mf.hasFace(clicked_face);
		if (has_face && min_faces >= mf.getFaces().size()) {
			// Refuse to remove beyond minimum face count
			return null;
		}

		// Toggle clicked block face
		mf.setFace(clicked_face, !has_face);

		// Choose sound
		return has_face ? Sound.BLOCK_GRINDSTONE_USE : Sound.UI_STONECUTTER_TAKE_RESULT;
	}

	private Sound change_wall(final Player player, final Block block, final Wall wall, final BlockFace clicked_face) {
		// Trace which side is the dominant side
		final var result = raytrace_dominant_face(player, block);
		if (result == null) {
			return null;
		}

		final BlockFace adjusted_clicked_face;
		// Only replace facing choice if we did hit a side,
		// or if the dominance was big enough.
		if (result.dominance > .2 || (clicked_face != BlockFace.UP && clicked_face != BlockFace.DOWN)) {
			adjusted_clicked_face = result.face;
		} else {
			adjusted_clicked_face = clicked_face;
		}

		if (clicked_face == BlockFace.UP) {
			final var was_up = wall.isUp();
			if (adjusted_clicked_face == BlockFace.UP) {
				// click top in middle -> toggle up
				wall.setUp(!was_up);
			} else if (BlockUtil.XZ_FACES.contains(adjusted_clicked_face)) {
				// click top on side -> toggle height
				final var height = wall.getHeight(adjusted_clicked_face);
				switch (height) {
					case NONE:
						return null;
					case LOW:
						wall.setHeight(adjusted_clicked_face, Wall.Height.TALL);
						break;
					case TALL:
						wall.setHeight(adjusted_clicked_face, Wall.Height.LOW);
						break;
				}
			}

			return was_up ? Sound.BLOCK_GRINDSTONE_USE : Sound.UI_STONECUTTER_TAKE_RESULT;
		} else {
			// click side -> toggle side
			final var has_face = wall.getHeight(adjusted_clicked_face) != Wall.Height.NONE;

			// Set height and choose sound
			if (has_face) {
				wall.setHeight(adjusted_clicked_face, Wall.Height.NONE);
				return Sound.BLOCK_GRINDSTONE_USE;
			} else {
				// Use opposite face's height, or low if there is nothing.
				var target_height = wall.getHeight(adjusted_clicked_face.getOppositeFace());
				if (target_height == Wall.Height.NONE) {
					target_height = Wall.Height.LOW;
				}
				wall.setHeight(adjusted_clicked_face, target_height);
				return Sound.UI_STONECUTTER_TAKE_RESULT;
			}
		}
	}

	private Sound change_directional_facing(
		final Player player,
		final Block block,
		final Directional directional,
		final BlockFace clicked_face
	) {
		// Toggle facing
		directional.setFacing(next_facing(directional.getFaces(), directional.getFacing()));
		return Sound.UI_STONECUTTER_TAKE_RESULT;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_right_click(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		if (event.useItemInHand() == Event.Result.DENY) {
			return;
		}

		// Get item variant
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItem(event.getHand());
		if (!isInstance(item)) {
			return;
		}

		// Create a block break event for block to transmute and check if it gets canceled
		final var block = event.getClickedBlock();
		final var break_event = new BlockBreakEvent(block, player);
		get_module().getServer().getPluginManager().callEvent(break_event);
		if (break_event.isCancelled()) {
			return;
		}

		final var data = block.getBlockData();
		final var clicked_face = event.getBlockFace();

		final Sound sound;
		if (player.isSneaking()) {
			if (data instanceof Stairs) {
				sound = change_stair_half((Stairs) data);
			} else {
				return;
			}
		} else {
			if (data instanceof MultipleFacing) {
				sound = change_multiple_facing(player, block, (MultipleFacing) data, clicked_face);
			} else if (data instanceof Wall) {
				sound = change_wall(player, block, (Wall) data, clicked_face);
			} else if (data instanceof Stairs) {
				sound = change_stair_shape(player, block, (Stairs) data, clicked_face);
			} else {
				return;
			}
		}

		// Return if nothing was done
		if (sound == null) {
			return;
		}

		// Update block data, and don't trigger physics! (We do not want to affect surrounding blocks!)
		block.setBlockData(data, false);
		block.getWorld().playSound(block.getLocation(), sound, SoundCategory.BLOCKS, 1.0f, 1.0f);

		// Damage item and swing arm
		damage_item(player, item, 1);
		swing_arm(player, event.getHand());
	}

	@Override
	public Key itemType() {
		return Key.key(Key.MINECRAFT_NAMESPACE, "item/handheld");
	}
}
