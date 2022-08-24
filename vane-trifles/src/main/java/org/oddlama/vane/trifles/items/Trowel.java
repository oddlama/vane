package org.oddlama.vane.trifles.items;

import java.util.EnumSet;

import org.bukkit.Material;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.config.recipes.RecipeList;
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.api.InhibitBehavior;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "trowel", base = Material.WARPED_FUNGUS_ON_A_STICK, durability = 2000, model_data = 0x76000e, version = 1)
public class Trowel extends CustomItem<Trifles> {
	public Trowel(Context<Trifles> context) {
		super(context);
	}

	@Override
	public RecipeList default_recipes() {
		return RecipeList.of(new ShapedRecipeDefinition("generic")
			.shape("  s", "mm ")
			.set_ingredient('m', Material.IRON_INGOT)
			.set_ingredient('s', Material.STICK)
			.result(key().toString()));
	}

	@Override
	public EnumSet<InhibitBehavior> inhibitedBehaviors() {
		return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND);
	}
}
