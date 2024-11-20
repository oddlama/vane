package org.oddlama.vane.core.config.recipes;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmokingRecipe;
import org.oddlama.vane.util.ItemUtil;

public class CookingRecipeDefinition extends RecipeDefinition {

    private String input = null;
    private String result = null;
    private float experience = 0.0f;
    private int cooking_time = 10;
    private String type;

    public CookingRecipeDefinition(String name, String type) {
        super(name);
        this.type = type;
        switch (this.type) {
            case "blasting": // fallthrough
            case "furnace": // fallthrough
            case "campfire": // fallthrough
            case "smoking":
                break;
            default:
                throw new IllegalArgumentException("Invalid cooking recipe: Unknown type '" + this.type + "'");
        }
    }

    public CookingRecipeDefinition input(String input) {
        this.input = input;
        return this;
    }

    public CookingRecipeDefinition input(final Tag<?> tag) {
        return input("#" + tag.key());
    }

    public CookingRecipeDefinition input(Material material) {
        return input(material.key().toString());
    }

    public CookingRecipeDefinition result(String result) {
        this.result = result;
        return this;
    }

    @Override
    public Object to_dict() {
        final HashMap<String, Object> dict = new HashMap<>();
        dict.put("cooking_time", this.cooking_time);
        dict.put("experience", this.experience);
        dict.put("input", this.input);
        dict.put("result", this.result);
        dict.put("type", type);
        return dict;
    }

    @Override
    public RecipeDefinition from_dict(Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid " + type + " recipe dictionary: Argument must be a Map<String, Object>!"
            );
        }
        final var dict_map = (Map<?, ?>) dict;
        if (dict_map.get("input") instanceof String input) {
            this.input = input;
        } else {
            throw new IllegalArgumentException("Invalid " + type + " recipe dictionary: input must be a string");
        }

        if (dict_map.get("result") instanceof String result) {
            this.result = result;
        } else {
            throw new IllegalArgumentException("Invalid " + type + " recipe dictionary: result must be a string");
        }

        if (dict_map.get("experience") instanceof Float experience) {
            this.experience = experience;
        } else {
            throw new IllegalArgumentException("Invalid " + type + " recipe dictionary: experience must be a float");
        }

        if (dict_map.get("cooking_time") instanceof Integer cooking_time) {
            this.cooking_time = cooking_time;
        } else {
            throw new IllegalArgumentException("Invalid " + type + " recipe dictionary: cooking_time must be a int");
        }

        return this;
    }

    @Override
    public Recipe to_recipe(NamespacedKey base_key) {
        final var out = ItemUtil.itemstack_from_string(this.result).getLeft();
        final var in = RecipeDefinition.recipe_choice(input);
        switch (this.type) {
            case "blasting":
                return new BlastingRecipe(key(base_key), out, in, experience, cooking_time);
            case "furnace":
                return new FurnaceRecipe(key(base_key), out, in, experience, cooking_time);
            case "campfire":
                return new CampfireRecipe(key(base_key), out, in, experience, cooking_time);
            case "smoking":
                return new SmokingRecipe(key(base_key), out, in, experience, cooking_time);
            default:
                throw new IllegalArgumentException("Invalid cooking recipe: Unknown type '" + this.type + "'");
        }
    }
}
