package org.oddlama.vane.trifles.items;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;

import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "sickle")
public class Sickle extends CustomItem<Trifles> {
	public Sickle(Context<Trifles> context) {
		super(context);

		// TODO sickle variants .... how to name and stats...
		// TODO attack speed and ....

		((ShapedRecipe)add_recipe(recipe_key(), new ShapedRecipe(recipe_key(), item())))
			.shape(" x ", " x ", " x ")
			.setIngredient('x', Material.STICK);
		//add_recipe(recipe);
	}
}
