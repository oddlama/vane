package org.oddlama.vane.core.config.recipes;

import java.util.function.Supplier;
import org.bukkit.NamespacedKey;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDict;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleComponent;

public class Recipes<T extends Module<T>> extends ModuleComponent<T> {

    private final NamespacedKey base_recipe_key;

    @ConfigBoolean(
        def = true,
        desc = "Whether these recipes should be registered at all. Set to false to quickly disable all associated recipes."
    )
    public boolean config_register_recipes;

    @ConfigDict(cls = RecipeList.class, desc = "")
    private RecipeList config_recipes;

    private Supplier<RecipeList> def_recipes;
    private String desc;

    public Recipes(
        final Context<T> context,
        final NamespacedKey base_recipe_key,
        final Supplier<RecipeList> def_recipes
    ) {
        this(
            context,
            base_recipe_key,
            def_recipes,
            "The associated recipes. This is a map of recipe name to recipe definitions."
        );
    }

    public Recipes(
        final Context<T> context,
        final NamespacedKey base_recipe_key,
        final Supplier<RecipeList> def_recipes,
        final String desc
    ) {
        super(context);
        this.base_recipe_key = base_recipe_key;
        this.def_recipes = def_recipes;
        this.desc = desc;
    }

    public RecipeList config_recipes_def() {
        return def_recipes.get();
    }

    public String config_recipes_desc() {
        return desc;
    }

    @Override
    public void on_config_change() {
        // Recipes are processed in on_config_change and not in on_disable() / on_enable(),
        // as the current recipes need to be removed even if we are disabled afterwards.
        config_recipes.recipes().forEach(recipe -> get_module().getServer().removeRecipe(recipe.key(base_recipe_key)));
        if (enabled() && config_register_recipes) {
            config_recipes
                .recipes()
                .forEach(recipe -> get_module().getServer().addRecipe(recipe.to_recipe(base_recipe_key)));
        }
    }

    @Override
    protected void on_enable() {}

    @Override
    protected void on_disable() {}
}
