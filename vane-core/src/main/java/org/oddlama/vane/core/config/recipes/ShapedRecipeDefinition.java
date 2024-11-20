package org.oddlama.vane.core.config.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.oddlama.vane.util.ItemUtil;

public class ShapedRecipeDefinition extends RecipeDefinition {

    private List<String> shape = new ArrayList<>();
    private Map<String, String> ingredients = new HashMap<>();
    private String result = null;

    public ShapedRecipeDefinition(String name) {
        super(name);
    }

    public ShapedRecipeDefinition shape(String... shape) {
        this.shape = List.of(shape);
        return this;
    }

    public ShapedRecipeDefinition set_ingredient(char id, String ingredient) {
        this.ingredients.put("" + id, ingredient);
        return this;
    }

    public ShapedRecipeDefinition set_ingredient(char id, final Tag<?> tag) {
        return set_ingredient(id, "#" + tag.key());
    }

    public ShapedRecipeDefinition set_ingredient(char id, Material material) {
        return set_ingredient(id, material.key().toString());
    }

    public ShapedRecipeDefinition result(String result) {
        this.result = result;
        return this;
    }

    @Override
    public Object to_dict() {
        final HashMap<String, Object> dict = new HashMap<>();
        dict.put("type", "shaped");
        dict.put("shape", this.shape);
        dict.put("ingredients", this.ingredients);
        dict.put("result", this.result);
        return dict;
    }

    @Override
    public RecipeDefinition from_dict(Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid shaped recipe dictionary: Argument must be a Map<String, Object>!"
            );
        }
        final var dict_map = (Map<?, ?>) dict;
        if (dict_map.get("shape") instanceof List<?> shape) {
            this.shape = shape.stream().map(row -> (String) row).toList();
            if (this.shape.size() < 1 && this.shape.size() > 3) {
                throw new IllegalArgumentException(
                    "Invalid shaped recipe dictionary: shape must be a list of 1 to 3 strings"
                );
            }
        } else {
            throw new IllegalArgumentException("Invalid shaped recipe dictionary: shape must be a list of strings");
        }

        if (dict_map.get("ingredients") instanceof Map<?, ?> ingredients) {
            this.ingredients = ingredients
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> (String) e.getKey(), e -> (String) e.getValue()));
        } else {
            throw new IllegalArgumentException(
                "Invalid shaped recipe dictionary: ingredients must be a mapping of string to string"
            );
        }

        if (dict_map.get("result") instanceof String result) {
            this.result = result;
        } else {
            throw new IllegalArgumentException("Invalid shaped recipe dictionary: result must be a string");
        }

        return this;
    }

    @Override
    public Recipe to_recipe(NamespacedKey base_key) {
        final var recipe = new ShapedRecipe(key(base_key), ItemUtil.itemstack_from_string(this.result).getLeft());
        recipe.shape(this.shape.toArray(new String[0]));
        this.ingredients.forEach((name, definition) ->
                recipe.setIngredient(name.charAt(0), RecipeDefinition.recipe_choice(definition))
            );
        return recipe;
    }
}
