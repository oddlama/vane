package org.oddlama.vane.trifles.items;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;

import org.oddlama.vane.annotation.item.VaneItem;
import org.oddlama.vane.core.item.CustomItem;
import org.oddlama.vane.core.item.CustomItemVariant;
import org.oddlama.vane.core.item.ItemVariantEnum;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.trifles.Trifles;

@VaneItem(name = "sickle")
public class Sickle extends CustomItem<Trifles, Sickle> {
	public static enum Variant implements ItemVariantEnum {
		WOODEN_SICKLE("wooden", 0),
		STONE_SICKLE("stone", 1),
		IRON_SICKLE("iron", 2, false),
		GOLDEN_SICKLE("golden", 3),
		DIAMOND_SICKLE("diamond", 4),
		NETHERITE_SICKLE("netherite", 5);

		private String identifier;
		private int id;
		private boolean enabled;
		private Variant(String identifier, int id) {
			this(identifier, id, true);
		}

		private Variant(String identifier, int id, boolean enabled) {
			this.identifier = identifier;
			this.id = id;
			this.enabled = enabled;
		}

		@Override public int id() { return id; }
		@Override public String identifier() { return identifier; }
		@Override public boolean enabled() { return enabled; }
	}

	public static class SickleVariant extends CustomItemVariant<Trifles, Sickle, Variant> {
		public SickleVariant(Sickle parent, Variant variant) {
			super(parent, variant);
		}
	}

	public Sickle(Context<Trifles> context) {
		super(context, Variant.values(), SickleVariant::new);

		// TODO attack speed and ....

		//((ShapedRecipe)add_recipe(recipe_key(), new ShapedRecipe(recipe_key(), item())))
		//	.shape(" x ", " x ", " x ")
		//	.setIngredient('x', Material.STICK);
		//add_recipe(recipe);
	}
}
