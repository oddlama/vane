package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.LegacyKeys

/**
 * Recipe definition for shaped crafting recipes.
 *
 * @param name logical recipe name.
 */
class ShapedRecipeDefinition(name: String?) : RecipeDefinition(name) {
    /** Recipe shape rows. */
    private var shape: MutableList<String> = mutableListOf()

    /** Ingredient mapping from shape symbols to ingredient definitions. */
    private var ingredients: MutableMap<String?, String?> = mutableMapOf()

    /** Result item definition string. */
    private var result: String? = null

    /** Sets the recipe shape rows. */
    fun shape(vararg shape: String): ShapedRecipeDefinition {
        this.shape = mutableListOf(*shape)
        return this
    }

    /** Sets ingredient definition for a shape symbol. */
    fun setIngredient(id: Char, ingredient: String?): ShapedRecipeDefinition {
        this.ingredients[id.toString()] = ingredient
        return this
    }

    /** Sets ingredient definition from a Bukkit tag. */
    fun setIngredient(id: Char, tag: Tag<*>): ShapedRecipeDefinition {
        return setIngredient(id, "#" + tag.key())
    }

    /** Sets ingredient definition from a material key. */
    fun setIngredient(id: Char, material: Material): ShapedRecipeDefinition {
        return setIngredient(id, material.key().toString())
    }

    /** Sets result item definition string. */
    fun result(result: String?): ShapedRecipeDefinition {
        this.result = result
        return this
    }

    /** Serializes this recipe definition to dictionary form. */
    override fun toDict(): Any {
        val dict = mutableMapOf<String?, Any?>()
        dict["Type"] = "shaped"
        dict["Shape"] = this.shape
        dict["Ingredients"] = this.ingredients
        dict["Result"] = this.result
        return dict
    }

    /** Loads this recipe definition from dictionary form. */
    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) {
            "Invalid shaped recipe dictionary: Argument must be a Map<String, Object>!"
        }
        val shapeObj = LegacyKeys.lookup(dict, "Shape")
        if (shapeObj is MutableList<*>) {
            this.shape = shapeObj.filterIsInstance<String>().toMutableList()
            require(this.shape.size in 1..3) {
                "Invalid shaped recipe dictionary: shape must be a list of 1 to 3 strings"
            }
        } else {
            throw IllegalArgumentException(
                "Invalid shaped recipe dictionary: shape must be a list of strings"
            )
        }

        val ingredientsObj = LegacyKeys.lookup(dict, "Ingredients")
        if (ingredientsObj is MutableMap<*, *>) {
            this.ingredients =
                ingredientsObj.entries
                    .associate { e -> (e.key as String?) to (e.value as String?) }
                    .toMutableMap()
        } else {
            throw IllegalArgumentException(
                "Invalid shaped recipe dictionary: ingredients must be a mapping of string to string"
            )
        }

        val resultObj = LegacyKeys.lookup(dict, "Result")
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException(
                "Invalid shaped recipe dictionary: result must be a string"
            )
        }

        return this
    }

    /** Converts this definition into a Bukkit [ShapedRecipe]. */
    override fun toRecipe(baseKey: NamespacedKey?): Recipe {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val recipe = ShapedRecipe(key(bk), ItemUtil.itemstackFromString(this.result!!).first!!)
        recipe.shape(*this.shape.toTypedArray())
        this.ingredients.forEach { (name, definition) ->
            recipe.setIngredient(name!![0], recipeChoice(definition!!))
        }
        return recipe
    }
}
