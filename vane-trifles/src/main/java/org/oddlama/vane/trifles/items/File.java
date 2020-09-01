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
import org.bukkit.inventory.SmithingRecipe;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.Tag;
import java.util.UUID;
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
	}
}
