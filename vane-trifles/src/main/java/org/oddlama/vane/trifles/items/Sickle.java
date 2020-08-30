package org.oddlama.vane.trifles.items;

import org.oddlama.vane.trifles.Trifles;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemDamageEvent;

import org.oddlama.vane.annotation.enchantment.Rarity;
import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.item.CustomItem;

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
