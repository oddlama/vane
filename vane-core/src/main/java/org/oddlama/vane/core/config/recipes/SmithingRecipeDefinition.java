package org.oddlama.vane.core.config.recipes;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.oddlama.vane.util.ItemUtil;

public class SmithingRecipeDefinition extends RecipeDefinition {
	private String base = null;
	private String addition = null;
	private boolean copy_nbt = false;
	private String result = null;

	public SmithingRecipeDefinition(String name) {
		super(name);
	}

	public SmithingRecipeDefinition base(String base) {
		this.base = base;
		return this;
	}

	public SmithingRecipeDefinition base(final Tag<?> tag) {
		return base("#" + tag.key());
	}

	public SmithingRecipeDefinition base(Material material) {
		return base(material.key().toString());
	}

	public SmithingRecipeDefinition addition(String addition) {
		this.addition = addition;
		return this;
	}

	public SmithingRecipeDefinition copy_nbt(boolean copy_nbt) {
		this.copy_nbt = copy_nbt;
		return this;
	}

	public SmithingRecipeDefinition addition(final Tag<?> tag) {
		return addition("#" + tag.key());
	}

	public SmithingRecipeDefinition addition(Material material) {
		return addition(material.key().toString());
	}

	public SmithingRecipeDefinition result(String result) {
		this.result = result;
		return this;
	}

	@Override
	public Object to_dict() {
		final HashMap<String, Object> dict = new HashMap<>();
		dict.put("base", this.base);
		dict.put("addition", this.addition);
		dict.put("copy_nbt", this.copy_nbt);
		dict.put("result", this.result);
		dict.put("type", "smithing");
		return dict;
	}

	@Override
	public RecipeDefinition from_dict(Object dict) {
		if (!(dict instanceof Map<?,?>)) {
			throw new IllegalArgumentException("Invalid smithing recipe dictionary: Argument must be a Map<String, Object>!");
		}
		final var dict_map = (Map<?,?>)dict;
		if (dict_map.get("base") instanceof String base) {
			this.base = base;
		} else {
			throw new IllegalArgumentException("Invalid smithing recipe dictionary: base must be a string");
		}

		if (dict_map.get("addition") instanceof String addition) {
			this.addition = addition;
		} else {
			throw new IllegalArgumentException("Invalid smithing recipe dictionary: addition must be a string");
		}

		if (dict_map.get("copy_nbt") instanceof Boolean copy_nbt) {
			this.copy_nbt = copy_nbt;
		} else {
			throw new IllegalArgumentException("Invalid smithing recipe dictionary: copy_nbt must be a bool");
		}

		if (dict_map.get("result") instanceof String result) {
			this.result = result;
		} else {
			throw new IllegalArgumentException("Invalid smithing recipe dictionary: result must be a string");
		}

		return this;
	}

	@Override
	public Recipe to_recipe(NamespacedKey base_key) {
		return new SmithingTransformRecipe(key(base_key), ItemUtil.itemstack_from_string(this.result).getLeft(), new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE), RecipeDefinition.recipe_choice(base), RecipeDefinition.recipe_choice(addition), copy_nbt);
	}
}
