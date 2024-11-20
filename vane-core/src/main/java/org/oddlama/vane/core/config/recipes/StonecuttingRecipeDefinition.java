package org.oddlama.vane.core.config.recipes;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.StonecuttingRecipe;
import org.oddlama.vane.util.ItemUtil;

public class StonecuttingRecipeDefinition extends RecipeDefinition {

    private String input = null;
    private String result = null;

    public StonecuttingRecipeDefinition(String name) {
        super(name);
    }

    public StonecuttingRecipeDefinition input(String input) {
        this.input = input;
        return this;
    }

    public StonecuttingRecipeDefinition input(final Tag<?> tag) {
        return input("#" + tag.key());
    }

    public StonecuttingRecipeDefinition input(Material material) {
        return input(material.key().toString());
    }

    public StonecuttingRecipeDefinition result(String result) {
        this.result = result;
        return this;
    }

    @Override
    public Object to_dict() {
        final HashMap<String, Object> dict = new HashMap<>();
        dict.put("input", this.input);
        dict.put("result", this.result);
        dict.put("type", "stonecutting");
        return dict;
    }

    @Override
    public RecipeDefinition from_dict(Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid stonecutting recipe dictionary: Argument must be a Map<String, Object>!"
            );
        }
        final var dict_map = (Map<?, ?>) dict;
        if (dict_map.get("input") instanceof String input) {
            this.input = input;
        } else {
            throw new IllegalArgumentException("Invalid stonecutting recipe dictionary: input must be a string");
        }

        if (dict_map.get("result") instanceof String result) {
            this.result = result;
        } else {
            throw new IllegalArgumentException("Invalid stonecutting recipe dictionary: result must be a string");
        }

        return this;
    }

    @Override
    public Recipe to_recipe(NamespacedKey base_key) {
        final var out = ItemUtil.itemstack_from_string(this.result).getLeft();
        final var in = RecipeDefinition.recipe_choice(input);
        return new StonecuttingRecipe(key(base_key), out, in);
    }
}
