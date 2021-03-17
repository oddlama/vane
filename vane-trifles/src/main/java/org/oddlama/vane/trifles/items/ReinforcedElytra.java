package org.oddlama.vane.trifles.items;

import static org.oddlama.vane.util.ItemUtil.damage_item;
import static org.oddlama.vane.util.PlayerUtil.swing_arm;
import static org.oddlama.vane.util.Util.ms_to_ticks;
import static org.oddlama.vane.core.item.CustomItem.convert_existing;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.RecipeChoice.MaterialChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingRecipe;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.oddlama.vane.core.ResourcePackGenerator;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.enchantments.items.AncientTomeOfKnowledge;
import org.oddlama.vane.enchantments.items.BookVariant;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "reinforced_elytra")
public class ReinforcedElytra extends CustomItem<Trifles, ReinforcedElytra> {
	public static final UUID MODIFIER_UUID_REINFORCED_ELYTRA_DEFENSE = UUID.fromString("8d3a5a3c-06d4-40c5-be66-41ebf6a46435"); // Self-generated; Must always be the same!

	public static enum Variant implements ItemVariantEnum {
		NETHERITE;

		@Override public String prefix() { return name().toLowerCase(); }
		@Override public boolean enabled() { return true; }
	}

	public static class ReinforcedElytraVariant extends CustomItemVariant<Trifles, ReinforcedElytra, Variant> {
		@ConfigDouble(def = 6.0, min = 0, desc = "Amount of defense points.")
		private double config_defense_points;

		public ReinforcedElytraVariant(ReinforcedElytra parent, Variant variant) {
			super(parent, variant);
		}

		@Override
		public void register_recipes() {
			// We need to register a dummy recipe so we can change the result later.
			final var recipe = new SmithingRecipe(recipe_key(), item(),
					new MaterialChoice(Material.ELYTRA),
					new MaterialChoice(Material.NETHERITE_INGOT));
			add_recipe(recipe);
		}

		@Override
		public Material base() {
			return Material.ELYTRA;
		}

		public double config_defense_points_def() {
			switch (variant()) {
				default:        throw new RuntimeException("Missing variant case. This is a bug.");
				case NETHERITE: return 6.0;
			}
		}

		@Override
		public ItemStack modify_item_stack(ItemStack item) {
			final var meta = item.getItemMeta();
			final var modifier_defense = new AttributeModifier(MODIFIER_UUID_REINFORCED_ELYTRA_DEFENSE, "Defense", config_defense_points, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST);
			meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier_defense);
			meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier_defense);
			item.setItemMeta(meta);
			return item;
		}

		@Override
		public void on_generate_resource_pack(final ResourcePackGenerator pack) throws IOException {
			// Add normal variant
			//final var resource_name = "items/" + variant_name() + ".png";
			//final var resource = get_module().getResource(resource_name);
			//if (resource == null) {
			//	throw new RuntimeException("Missing resource '" + resource_name + "'. This is a bug.");
			//}
			//pack.add_item_model(key(), resource);
			//pack.add_item_override(base().getKey(), key(), predicate -> {
			//	predicate.put("custom_model_data", model_data());
			//	predicate.put("broken", 0);
			//});
			super.on_generate_resource_pack(pack);

			// Add broken variant
			final var key_broken = key("broken");
			final var broken_resource_name = "items/broken_" + variant_name() + ".png";
			final var broken_resource = get_module().getResource(broken_resource_name);
			if (broken_resource == null) {
				throw new RuntimeException("Missing resource '" + broken_resource_name + "'. This is a bug.");
			}
			pack.add_item_model(key_broken, broken_resource);
			pack.add_item_override(base().getKey(), key_broken, predicate -> {
				predicate.put("custom_model_data", model_data());
				predicate.put("broken", 1);
			});
		}
	}

	public ReinforcedElytra(Context<Trifles> context) {
		super(context, Variant.class, Variant.values(), ReinforcedElytraVariant::new);
	}

	// Prevent custom items from forming netherite variants, or delegate event to custom item
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void on_prepare_smithing(final PrepareSmithingEvent event) {
		final var item = event.getInventory().getInputEquipment();
		// Require elytra as input
		if (item == null || item.getType() != Material.ELYTRA) {
			return;
		}

		// Check netherite ingot ingredient
		final var mineral = event.getInventory().getInputMineral();
		if (mineral == null || mineral.getType() != Material.NETHERITE_INGOT) {
			return;
		}

		// Convert existing item
		event.setResult(convert_existing(item, this, Variant.NETHERITE));
	}
}
