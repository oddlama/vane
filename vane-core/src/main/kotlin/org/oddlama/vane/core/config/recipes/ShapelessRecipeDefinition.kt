package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapelessRecipe
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.LegacyKeys

/**
 * Recipe definition for shapeless crafting recipes.
 *
 * @param name logical recipe name.
 */
class ShapelessRecipeDefinition(name: String?) : RecipeDefinition(name) {
    /** Ingredient definition strings. */
    private var ingredients: MutableList<String> = ArrayList()

    /** Result item definition string. */
    private var result: String? = null

    /** Adds an ingredient definition string. */
    fun addIngredient(ingredient: String): ShapelessRecipeDefinition {
        this.ingredients.add(ingredient)
        return this
    }

    /** Adds an ingredient definition from a Bukkit tag. */
    fun addIngredient(tag: Tag<*>): ShapelessRecipeDefinition {
        return addIngredient("#" + tag.key())
    }

    /** Adds an ingredient definition from a material key. */
    fun addIngredient(material: Material): ShapelessRecipeDefinition {
        return addIngredient(material.key().toString())
    }

    /** Sets the result item definition string. */
    fun result(result: String?): ShapelessRecipeDefinition {
        this.result = result
        return this
    }

    /** Serializes this recipe definition to dictionary form. */
    override fun toDict(): Any {
        val dict = mutableMapOf<String?, Any?>()
        dict["Type"] = "shapeless"
        dict["Ingredients"] = this.ingredients
        dict["Result"] = this.result
        return dict
    }

    /** Loads this recipe definition from dictionary form. */
    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) {
            "Invalid shapeless recipe dictionary: Argument must be a Map<String, Object>!"
        }
        val ingredientsObj = LegacyKeys.lookup(dict, "Ingredients")
        if (ingredientsObj is MutableList<*>) {
            this.ingredients = ingredientsObj.filterIsInstance<String>().toMutableList()
        } else {
            throw IllegalArgumentException(
                "Invalid shapeless recipe dictionary: ingredients must be a list of strings"
            )
        }

        val resultObj = LegacyKeys.lookup(dict, "Result")
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException(
                "Invalid shapeless recipe dictionary: result must be a string"
            )
        }

        return this
    }

    /** Converts this definition into a Bukkit [ShapelessRecipe]. */
    override fun toRecipe(baseKey: NamespacedKey?): Recipe {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val recipe = ShapelessRecipe(key(bk), ItemUtil.itemstackFromString(this.result!!).first!!)
        this.ingredients.forEach { i -> recipe.addIngredient(recipeChoice(i)) }
        return recipe
    }
}
