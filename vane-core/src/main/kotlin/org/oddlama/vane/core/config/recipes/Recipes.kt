package org.oddlama.vane.core.config.recipes

import org.bukkit.NamespacedKey
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigDict
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.module.ModuleComponent

/**
 * Config-backed recipe registration component.
 *
 * @param T owning module type.
 * @param context component context.
 * @param baseRecipeKey base key prefix for registered recipes.
 * @param defRecipes supplier for default recipes.
 * @param desc optional config description for the recipes' dictionary.
 */
class Recipes<T : Module<T?>?>
@JvmOverloads
constructor(
    context: Context<T?>?,
    /** Base key prefix for all registered recipes. */
    private val baseRecipeKey: NamespacedKey,
    /** Supplier of default recipe definitions. */
    private val defRecipes: () -> RecipeList?,
    /** Optional config description for recipe dictionary. */
    private val desc: String? =
        "The associated recipes. This is a map of recipe name to recipe definitions.",
) : ModuleComponent<T?>(context) {
    /** Whether recipe registration is enabled. */
    @ConfigBoolean(
        def = true,
        desc =
            "Whether these recipes should be registered at all. Set to false to quickly disable all associated recipes.",
    )
    var configRegisterRecipes: Boolean = false

    /** Recipe configuration dictionary. */
    @ConfigDict(cls = RecipeList::class, desc = "") private var configRecipes: RecipeList? = null

    /** Returns default recipe definitions for config generation. */
    fun configRecipesDef(): RecipeList? = defRecipes()

    /** Returns config description text for the recipe dictionary. */
    fun configRecipesDesc(): String? = desc

    /** Re-registers recipes according to current config state. */
    override fun onConfigChange() {
        configRecipes!!.recipes().forEach { recipe ->
            module!!.server.removeRecipe(recipe!!.key(baseRecipeKey))
        }
        if (enabled() && configRegisterRecipes) {
            configRecipes!!.recipes().forEach { recipe ->
                module!!.server.addRecipe(recipe!!.toRecipe(baseRecipeKey))
            }
        }
    }

    /** Enables this component. */
    override fun onEnable() {}

    /** Disables this component. */
    override fun onDisable() {}
}
