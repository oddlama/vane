package org.oddlama.vane.core.config.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.oddlama.vane.util.ItemUtil;

public class ShapelessRecipeDefinition extends RecipeDefinition {
	private List<String> ingredients = new ArrayList<>();
	private String result = null;

	public ShapelessRecipeDefinition(String name) {
		super(name);
	}

	public ShapelessRecipeDefinition add_ingredient(String ingredient) {
		this.ingredients.add(ingredient);
		return this;
	}

	public ShapelessRecipeDefinition add_ingredient(final Tag<?> tag) {
		return add_ingredient("#" + tag.key());
	}

	public ShapelessRecipeDefinition add_ingredient(Material material) {
		return add_ingredient(material.key().toString());
	}

	public ShapelessRecipeDefinition result(String result) {
		this.result = result;
		return this;
	}

	@Override
	public Object to_dict() {
		final HashMap<String, Object> dict = new HashMap<>();
		dict.put("type", "shapeless");
		dict.put("ingredients", this.ingredients);
		dict.put("result", this.result);
		return dict;
	}

	@Override
	public RecipeDefinition from_dict(Object dict) {
		if (!(dict instanceof Map<?,?>)) {
			throw new IllegalArgumentException("Invalid shapeless recipe dictionary: Argument must be a Map<String, Object>!");
		}
		final var dict_map = (Map<?,?>)dict;
		if (dict_map.get("ingredients") instanceof List<?> ingredients) {
			this.ingredients = ingredients.stream().map(i -> (String)i).toList();
		} else {
			throw new IllegalArgumentException("Invalid shapeless recipe dictionary: ingredients must be a list of strings");
		}

		if (dict_map.get("result") instanceof String result) {
			this.result = result;
		} else {
			throw new IllegalArgumentException("Invalid shapeless recipe dictionary: result must be a string");
		}

		return this;
	}

	@Override
	public Recipe to_recipe(NamespacedKey base_key) {
		final var recipe = new ShapelessRecipe(key(base_key), ItemUtil.itemstack_from_string(this.result).getLeft());
		this.ingredients.forEach(i -> recipe.addIngredient(RecipeDefinition.recipe_choice(i)));
		return recipe;
	}
}
