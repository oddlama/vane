package org.oddlama.vane.trifles.items;

import org.bukkit.Material;
import org.oddlama.vane.util.BlockUtil;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_DAMAGE;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_SPEED;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.PlayerUtil.harvest_plant;
import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import static org.oddlama.vane.util.BlockUtil.raytrace_dominant_face;
import static org.oddlama.vane.util.BlockUtil.raytrace_oct;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.SmithingRecipe;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.Tag;
import java.util.UUID;
import java.util.Collections;
import java.util.Set;
import java.util.ArrayList;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.attribute.AttributeModifier;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Tripwire;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "file")
public class File extends CustomItem<Trifles, File> {
	public static enum Variant implements ItemVariantEnum {
		WOODEN,
		STONE,
		IRON,
		GOLDEN,
		DIAMOND,
		NETHERITE;

		@Override public String prefix() { return name().toLowerCase(); }
		@Override public boolean enabled() { return true; }
	}

	public static class FileVariant extends CustomItemVariant<Trifles, File, Variant> {
		@ConfigDouble(def = Double.NaN, desc = "Attack damage modifier.")
		public double config_attack_damage;
		@ConfigDouble(def = Double.NaN, desc = "Attack speed modifier.")
		public double config_attack_speed;

		public FileVariant(File parent, Variant variant) {
			super(parent, variant);
		}

		@Override
		public void register_recipes() {
			final var recipe_key = recipe_key();
			if (variant() == Variant.NETHERITE) {
				// TODO add_recipe(recipe_key, new SmithingRecipe(recipe_key, item(), item(Variant.DIAMOND), Material.NETHERITE_INGOT));
			} else {
				final var recipe = new ShapedRecipe(recipe_key, item())
					.shape(" m",
						   "s ")
					.setIngredient('s', Material.STICK);

				switch (variant()) {
					case WOODEN:    recipe.setIngredient('m', new MaterialChoice(Tag.PLANKS)); break;
					case STONE:     recipe.setIngredient('m', new MaterialChoice(Tag.ITEMS_STONE_TOOL_MATERIALS)); break;
					case IRON:      recipe.setIngredient('m', Material.IRON_INGOT); break;
					case GOLDEN:    recipe.setIngredient('m', Material.GOLD_INGOT); break;
					case DIAMOND:   recipe.setIngredient('m', Material.DIAMOND); break;
					case NETHERITE: /* Can't happen */ break;
				}

				add_recipe(recipe_key, recipe);
			}
		}

		@Override
		public Material base() {
			switch (variant()) {
				default:        throw new RuntimeException("Missing variant case. This is a bug.");
				case WOODEN:    return Material.WOODEN_HOE;
				case STONE:     return Material.STONE_HOE;
				case IRON:      return Material.IRON_HOE;
				case GOLDEN:    return Material.GOLDEN_HOE;
				case DIAMOND:   return Material.DIAMOND_HOE;
				case NETHERITE: return Material.NETHERITE_HOE;
			}
		}

		public double config_attack_damage_def() {
			switch (variant()) {
				default:        throw new RuntimeException("Missing variant case. This is a bug.");
				case WOODEN:    return 1.0;
				case STONE:     return 1.0;
				case IRON:      return 1.0;
				case GOLDEN:    return 1.0;
				case DIAMOND:   return 2.0;
				case NETHERITE: return 3.0;
			}
		}

		public double config_attack_speed_def() {
			switch (variant()) {
				default:        throw new RuntimeException("Missing variant case. This is a bug.");
				case WOODEN:    return 2.0;
				case STONE:     return 3.0;
				case IRON:      return 4.0;
				case GOLDEN:    return 6.0;
				case DIAMOND:   return 5.0;
				case NETHERITE: return 5.0;
			}
		}

		@Override
		public ItemStack modify_item_stack(ItemStack item) {
			final var meta = item.getItemMeta();
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(MODIFIER_UUID_GENERIC_ATTACK_DAMAGE, "Tool damage", config_attack_damage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(MODIFIER_UUID_GENERIC_ATTACK_SPEED, "Tool speed", config_attack_speed, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND));
			item.setItemMeta(meta);
			return item;
		}
	}

	public File(Context<Trifles> context) {
		super(context, Variant.class, Variant.values(), FileVariant::new);
	}

	private Bisected.Half other_half(Bisected.Half h) {
		switch (h) {
			default:
			case BOTTOM: return Bisected.Half.TOP;
			case TOP:    return Bisected.Half.BOTTOM;
		}
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
			default:    return null;
			case NORTH: return BlockFace.WEST;
			case EAST:  return BlockFace.NORTH;
			case SOUTH: return BlockFace.EAST;
			case WEST:  return BlockFace.SOUTH;
		}
	}

	private Sound change_stair_shape(final Player player, final Block block, final Stairs stairs, final BlockFace clicked_face) {
		// Check which eighth of the block was clicked
		final var oct = raytrace_oct(player, block);
		if (oct == null) {
			return null;
		}

		var corner = oct.corner();
		final var is_stair_top = stairs.getHalf() == Bisected.Half.TOP;

		// If we clicked on the base part, toggle the corner up state,
		// as we always want to reason abount the corner to add/remove.
		if (corner.up() == is_stair_top) {
			corner = corner.up(!corner.up());
		}

		// Rotate corner so that we can interpret it from
		// the reference facing (north)
		final var original_facing = stairs.getFacing();
		corner = corner.rotate_to_north_reference(original_facing);

		// Determine resulting shape, face and wether the oct got added or removed
		Stairs.Shape shape = null;
		BlockFace face = null;
		boolean added = false;
		switch (stairs.getShape()) {
			case STRAIGHT:
				switch (corner.xz_face()) {
					case SOUTH_WEST: shape = Stairs.Shape.INNER_LEFT;  face = BlockFace.NORTH; added = true;  break;
					case NORTH_WEST: shape = Stairs.Shape.OUTER_RIGHT; face = BlockFace.NORTH; added = false; break;
					case NORTH_EAST: shape = Stairs.Shape.OUTER_LEFT;  face = BlockFace.NORTH; added = false; break;
					case SOUTH_EAST: shape = Stairs.Shape.INNER_RIGHT; face = BlockFace.NORTH; added = true;  break;
				}
				break;
			case INNER_LEFT:
				switch (corner.xz_face()) {
					case SOUTH_WEST: shape = Stairs.Shape.STRAIGHT;    face = BlockFace.NORTH; added = false; break;
					case NORTH_EAST: shape = Stairs.Shape.STRAIGHT;    face = BlockFace.WEST;  added = false; break;
				}
				break;
			case INNER_RIGHT:
				switch (corner.xz_face()) {
					case NORTH_WEST: shape = Stairs.Shape.STRAIGHT;    face = BlockFace.EAST;  added = false; break;
					case SOUTH_EAST: shape = Stairs.Shape.STRAIGHT;    face = BlockFace.NORTH; added = false; break;
				}
				break;
			case OUTER_LEFT:
				switch (corner.xz_face()) {
					case SOUTH_WEST: shape = Stairs.Shape.STRAIGHT;    face = BlockFace.WEST;  added = true;  break;
					case NORTH_EAST: shape = Stairs.Shape.STRAIGHT;    face = BlockFace.NORTH; added = true;  break;
				}
				break;
			case OUTER_RIGHT:
				switch (corner.xz_face()) {
					case NORTH_WEST: shape = Stairs.Shape.STRAIGHT;    face = BlockFace.NORTH; added = true;  break;
					case SOUTH_EAST: shape = Stairs.Shape.STRAIGHT;    face = BlockFace.EAST;  added = true;  break;
				}
				break;
		}

		// Break if the resulting shape is invalid
		if (shape == null || face == null) {
			return null;
		}

		// Undo reference rotation
		switch (face) {
			case NORTH: face = original_facing; break;
			case EAST:  face = next_face_ccw(original_facing).getOppositeFace(); break;
			case SOUTH: face = original_facing.getOppositeFace(); break;
			case WEST:  face = next_face_ccw(original_facing); break;
		}

		stairs.setShape(shape);
		stairs.setFacing(face);

		return added ? Sound.UI_STONECUTTER_TAKE_RESULT : Sound.BLOCK_GRINDSTONE_USE;
	}

	private Sound change_multiple_facing(final Player player, final Block block, final MultipleFacing mf, BlockFace clicked_face) {
		final int min_faces;
		if (mf instanceof Fence || mf instanceof GlassPane) {
			// Allow fences and glass panes to have 0 faces
			min_faces = 0;

			// Trace which side is the dominant side
			final var result = raytrace_dominant_face(player, block);
			if (result == null) {
				return null;
			}

			// Only replace facing choice, if we did hit a side,
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
		// Only replace facing choice, if we did hit a side,
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
					case NONE: return null;
					case LOW:  wall.setHeight(adjusted_clicked_face, Wall.Height.TALL); break;
					case TALL: wall.setHeight(adjusted_clicked_face, Wall.Height.LOW); break;
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
				wall.setHeight(adjusted_clicked_face, Wall.Height.LOW);
				return Sound.UI_STONECUTTER_TAKE_RESULT;
			}
		}
	}

	private Sound change_directional_facing(final Player player, final Block block, final Directional directional, final BlockFace clicked_face) {
		// Toggle facing
		directional.setFacing(next_facing(directional.getFaces(), directional.getFacing()));
		return Sound.UI_STONECUTTER_TAKE_RESULT;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void on_player_right_click(final PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Get item variant
		final var player = event.getPlayer();
		final var item = player.getEquipment().getItem(event.getHand());
		final var variant = this.<FileVariant>variant_of(item);
		if (variant == null || !variant.enabled()) {
			return;
		}

		// Create block break event for block to transmute and check if it gets cancelled
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
				sound = change_stair_half((Stairs)data);
			} else {
				return;
			}
		} else {
			if (data instanceof MultipleFacing) {
				sound = change_multiple_facing(player, block, (MultipleFacing)data, clicked_face);
			} else if (data instanceof Wall) {
				sound = change_wall(player, block, (Wall)data, clicked_face);
			} else if (data instanceof Stairs) {
				sound = change_stair_shape(player, block, (Stairs)data, clicked_face);
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
}
