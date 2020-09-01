package org.oddlama.vane.trifles.items;

import org.bukkit.Material;
import org.oddlama.vane.util.BlockUtil;
import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_DAMAGE;
import static org.oddlama.vane.util.ItemUtil.MODIFIER_UUID_GENERIC_ATTACK_SPEED;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.PlayerUtil.harvest_plant;
import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
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
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.RayTraceResult;
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

		Sound sound_effect = null;
		final var data = block.getBlockData();
		var clicked_face = event.getBlockFace();

		if (player.isSneaking()) {
			if (data instanceof Stairs) {
				// Toggle shapes and set bisected
				final var stairs = (Stairs)data;

				// Check which half was clicked
				final var hit_half = raytrace_stair_half(player, block);
				if (hit_half == null) {
					return;
				}

				if (hit_half != stairs.getHalf()) {
					// Change half
					stairs.setHalf(other_half(stairs.getHalf()));
				} else {
					// Change shape
					stairs.setShape(next_stair_shape(stairs.getShape()));
				}

				sound_effect = Sound.UI_STONECUTTER_TAKE_RESULT;
			} else if (data instanceof Bisected) {
				// Toggle bisected
				final var bisected = (Bisected)data;
				bisected.setHalf(other_half(bisected.getHalf()));
				sound_effect = Sound.UI_STONECUTTER_TAKE_RESULT;
			} else {
				return;
			}
		} else {
			if (data instanceof MultipleFacing) {
				final var mf = (MultipleFacing)data;
				// Change multiple facing
				if (!mf.getAllowedFaces().contains(clicked_face)) {
					return;
				}

				if (mf instanceof Fence) {
					// Do special fence raytracing
					final var result = raytrace_fence_like(player, block);
					if (result == null) {
						return;
					}

					// Only replace facing choice, if we did hit a side,
					// or if the max_change was big enough.
					if (result.max_change > .2 || (clicked_face != BlockFace.UP && clicked_face != BlockFace.DOWN)) {
						clicked_face = result.face;
					}
				}

				boolean has_face = mf.hasFace(clicked_face);
				if (mf.getFaces().size() == 1 && has_face) {
					// Refuse to remove the last remaining face
					return;
				}

				// Toggle clicked block face
				mf.setFace(clicked_face, !has_face);

				// Choose sound
				if (has_face) {
					sound_effect = Sound.BLOCK_GRINDSTONE_USE;
				} else {
					sound_effect = Sound.UI_STONECUTTER_TAKE_RESULT;
				}
			} else if (data instanceof Wall) {
				final var wall = (Wall)data;

				// Do special fence-like raytracing
				final var result = raytrace_fence_like(player, block);
				if (result == null) {
					return;
				}

				// Only replace facing choice, if we did hit a side,
				// or if the max_change was big enough.
				if (result.max_change > .2 || (clicked_face != BlockFace.UP && clicked_face != BlockFace.DOWN)) {
					clicked_face = result.face;
				}

				// click side -> toggle side
				// click top in middle -> toggle up
				// click top on side -> toggle height
				if (clicked_face == BlockFace.UP) {
					todo
				} else {
					final var has_face = wall.getHeight(clicked_face) != Wall.Height.NONE;
					var active_face_count = 0;
					for (final var face : BlockUtil.XZ_FACES) {
						active_face_count += (wall.getHeight(face) != Wall.Height.NONE ? 1 : 0);
					}

					if (has_face && active_face_count == 1) {
						// Refuse to remove the last remaining face
						return;
					}

					// Toggle clicked block face
					wall.setHeight(clicked_face, has_face ? Wall.Height.NONE : Wall.Height.LOW);

					// Choose sound
					if (has_face) {
						sound_effect = Sound.BLOCK_GRINDSTONE_USE;
					} else {
						sound_effect = Sound.UI_STONECUTTER_TAKE_RESULT;
					}
				}
			} else if (data instanceof Stairs) {
				// Toggle stair facing
				final var stairs = (Stairs)data;
				stairs.setFacing(next_facing(stairs.getFaces(), stairs.getFacing()));

				sound_effect = Sound.UI_STONECUTTER_TAKE_RESULT;
			} else {
				return;
			}
		}

		// Update block data, and don't trigger physics! (We do not want to affect surrounding blocks!)
		block.setBlockData(data, false);
		block.getWorld().playSound(block.getLocation(), sound_effect, SoundCategory.BLOCKS, 1.0f, 1.0f);

		// Damage item and swing arm
		damage_item(player, item, 1);
		swing_arm(player, event.getHand());
	}

	private Bisected.Half other_half(Bisected.Half h) {
		switch (h) {
			default:
			case BOTTOM: return Bisected.Half.TOP;
			case TOP:    return Bisected.Half.BOTTOM;
		}
	}

	private Stairs.Shape next_stair_shape(Stairs.Shape s) {
		return Stairs.Shape.values()[(s.ordinal() + 1) % Stairs.Shape.values().length];
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

	private Bisected.Half raytrace_stair_half(Player player, Block block) {
		// Ray trace clicked face
		final var result = player.rayTraceBlocks(10.0);
		if (!block.equals(result.getHitBlock())) {
			return null;
		}
		var y_val = result.getHitPosition().getY();
		y_val -= (int)y_val;
		if (y_val >= 0.5) {
			return Bisected.Half.TOP;
		}
		return Bisected.Half.BOTTOM;
	}

	private static class RayTraceFenceResult {
		public BlockFace face = null;
		public double max_change = 0.0;
	}

	private RayTraceFenceResult raytrace_fence_like(final Player player, final Block block) {
		// Ray trace clicked face
		final var result = player.rayTraceBlocks(10.0);
		if (!block.equals(result.getHitBlock())) {
			return null;
		}

		final var block_middle = block.getLocation().toVector().add(new Vector(0.5, 0.5, 0.5));
		final var hit_position = result.getHitPosition();
		final var diff = hit_position.subtract(block_middle);

		final var ret = new RayTraceFenceResult();
		for (final var face : BlockUtil.XZ_FACES) {
			if (face.getModX() != 0) {
				final var change = diff.getX() * face.getModX();
				if (change > ret.max_change) {
					ret.face = face;
					ret.max_change = change;
				}
			} else if (face.getModZ() != 0) {
				final var change = diff.getZ() * face.getModZ();
				if (change > ret.max_change) {
					ret.face = face;
					ret.max_change = change;
				}
			}
		}

		return ret;
	}
}
