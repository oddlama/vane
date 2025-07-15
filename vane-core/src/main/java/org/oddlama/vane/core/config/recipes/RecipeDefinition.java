package org.oddlama.vane.core.config.recipes;

import static org.oddlama.vane.util.MaterialUtil.material_from;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.util.ItemUtil;
import org.oddlama.vane.util.StorageUtil;

public abstract class RecipeDefinition {

    public String name;

    public RecipeDefinition(final String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public NamespacedKey key(final NamespacedKey base_key) {
        return StorageUtil.namespaced_key(base_key.namespace(), base_key.value() + "." + name);
    }

    public abstract Recipe to_recipe(final NamespacedKey base_key);

    public abstract Object to_dict();

    public abstract RecipeDefinition from_dict(Object dict);

    public static RecipeDefinition from_dict(final String name, final Object dict) {
        if (!(dict instanceof Map<?, ?>)) {
            throw new IllegalArgumentException(
                "Invalid recipe dictionary: Argument must be a Map<String, Object>, but is " + dict.getClass() + "!"
            );
        }
        final var type = ((Map<?, ?>) dict).get("type");
        if (!(type instanceof String)) {
            throw new IllegalArgumentException("Invalid recipe dictionary: recipe type must exist and be a string!");
        }

        final var str_type = (String) type;
        switch (str_type) {
            case "shaped":
                return new ShapedRecipeDefinition(name).from_dict(dict);
            case "shapeless":
                return new ShapelessRecipeDefinition(name).from_dict(dict);
            case "blasting": // fallthrough
            case "furnace": // fallthrough
            case "campfire": // fallthrough
            case "smoking":
                return new CookingRecipeDefinition(name, str_type).from_dict(dict);
            case "smithing":
                return new SmithingRecipeDefinition(name).from_dict(dict);
            case "stonecutting":
                return new StonecuttingRecipeDefinition(name).from_dict(dict);
            default:
                break;
        }

        throw new IllegalArgumentException("Unknown recipe type '" + str_type + "'");
    }

    @SuppressWarnings("unchecked")
    public static @NotNull RecipeChoice recipe_choice(String definition) {
        definition = definition.strip();

        // Try a material #tag
        if (definition.startsWith("#")) {
            for (final var f : Tag.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == Tag.class) {
                    try {
                        final var tag = (Tag<?>) f.get(null);
                        if (tag == null) {
                            // System.out.println("warning: " + f + " has no associated key! It
                            // therefore cannot be used in custom recipes.");
                            continue;
                        }
                        if (tag.key().toString().equals(definition.substring(1))) {
                            return new RecipeChoice.MaterialChoice((Tag<Material>) tag);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        throw new IllegalArgumentException("Invalid material tag: " + definition);
                    }
                }
            }
            throw new IllegalArgumentException("Unknown material tag: " + definition);
        }

        // Tuple of materials
        if (definition.startsWith("(") && definition.endsWith(")")) {
            final var parts = Arrays.stream(definition.substring(1, definition.length() - 1).split(","))
                .map(key -> {
                    final var mat = material_from(NamespacedKey.fromString(key.strip()));
                    if (mat == null) {
                        throw new IllegalArgumentException(
                            "Unknown material (only normal materials are allowed in tags): " + key
                        );
                    }
                    return mat;
                })
                .collect(Collectors.toList());
            return new RecipeChoice.MaterialChoice(parts);
        }

        // Check if the amount is included
        final var mult = definition.indexOf('*');
        int amount = 1;
        if (mult != -1) {
            final var amount_str = definition.substring(0, mult).strip();
            try {
                amount = Integer.parseInt(amount_str);
                if (amount <= 0) {
                    amount = 1;
                }

                // Remove amount from definition for parsing
                definition = definition.substring(mult + 1).strip();
            } catch (NumberFormatException e) {}
        }

        // Exact choice of itemstack including NBT
        final var item_stack_and_is_simple_mat = ItemUtil.itemstack_from_string(definition);
        final var item_stack = item_stack_and_is_simple_mat.getLeft();
        final var is_simple_mat = item_stack_and_is_simple_mat.getRight();
        if (is_simple_mat && amount == 1) {
            return new RecipeChoice.MaterialChoice(item_stack.getType());
        }

        item_stack.setAmount(amount);
        return new RecipeChoice.ExactChoice(item_stack);
    }
}
