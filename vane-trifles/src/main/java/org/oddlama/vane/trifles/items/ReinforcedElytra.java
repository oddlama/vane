package org.oddlama.vane.trifles.items;

import java.io.IOException;
import java.util.EnumSet;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator;
import org.oddlama.vane.trifles.Trifles;
import org.oddlama.vane.util.StorageUtil;

@VaneItem(name = "reinforced_elytra", base = Material.ELYTRA, durability = 864, model_data = 0x760002, version = 1)
public class ReinforcedElytra extends CustomItem<Trifles> {
	public static final UUID MODIFIER_UUID_REINFORCED_ELYTRA_DEFENSE = UUID.fromString("8d3a5a3c-06d4-40c5-be66-41ebf6a46435"); // Self-generated; Must always be the same!

	@ConfigDouble(def = 6.0, min = 0, desc = "Amount of defense points.")
	private double config_defense_points;

	public ReinforcedElytra(Context<Trifles> context) {
		super(context);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new SmithingRecipeDefinition("generic")
			.base(Material.ELYTRA)
			.addition(Material.NETHERITE_INGOT)
			.copy_nbt(true)
			.result(key().toString()));
	}

	@Override
	public ItemStack updateItemStack(ItemStack item_stack) {
		item_stack.editMeta(meta -> {
			final var modifier_defense = new AttributeModifier(
				MODIFIER_UUID_REINFORCED_ELYTRA_DEFENSE,
				"Defense",
				config_defense_points,
				AttributeModifier.Operation.ADD_NUMBER,
				EquipmentSlot.CHEST
			);
			meta.removeAttributeModifier(Attribute.GENERIC_ARMOR, modifier_defense);
			meta.addAttributeModifier(Attribute.GENERIC_ARMOR, modifier_defense);
		});
		return item_stack;
	}

	@Override
	public void addResources(final ResourcePackGenerator rp) throws IOException {
		// Add normal variant
		super.addResources(rp);

		// Add broken variant
		final var broken_resource_name = "items/broken_" + key().value() + ".png";
		final var broken_resource = get_module().getResource(broken_resource_name);
		if (broken_resource == null) {
			throw new RuntimeException("Missing resource '" + broken_resource_name + "'. This is a bug.");
		}

		final var key_broken = StorageUtil.subkey(key(), "broken");
		rp.add_item_model(key_broken, broken_resource);
		rp.add_item_override(baseMaterial().getKey(), key_broken, predicate -> {
			predicate.put("custom_model_data", customModelData());
			predicate.put("broken", 1);
		});
	}

	@Override
	public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.ITEM_BURN);
	}
}
