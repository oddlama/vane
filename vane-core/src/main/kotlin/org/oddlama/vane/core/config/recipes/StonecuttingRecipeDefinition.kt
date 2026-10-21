package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.StonecuttingRecipe
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.LegacyKeys

/**
 * Recipe definition for stonecutting recipes.
 *
 * @param name logical recipe name.
 */
class StonecuttingRecipeDefinition(name: String?) : RecipeDefinition(name) {
    /** Input ingredient definition. */
    private var input: String? = null

    /** Result item definition string. */
    private var result: String? = null

    /** Sets input ingredient definition. */
    fun input(input: String?): StonecuttingRecipeDefinition {
        this.input = input
        return this
    }

    /** Sets input ingredient from a Bukkit tag. */
    fun input(tag: Tag<*>): StonecuttingRecipeDefinition {
        return input("#" + tag.key())
    }

    /** Sets input ingredient from a material key. */
    fun input(material: Material): StonecuttingRecipeDefinition {
        return input(material.key().toString())
    }

    /** Sets result item definition string. */
    fun result(result: String?): StonecuttingRecipeDefinition {
        this.result = result
        return this
    }

    /** Serializes this recipe definition to dictionary form. */
    override fun toDict(): Any {
        val dict = mutableMapOf<String?, Any?>()
        dict["Input"] = this.input
        dict["Result"] = this.result
        dict["Type"] = "stonecutting"
        return dict
    }

    /** Loads this recipe definition from dictionary form. */
    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) {
            "Invalid stonecutting recipe dictionary: Argument must be a Map<String, Object>!"
        }
        val inputObj = LegacyKeys.lookup(dict, "Input")
        if (inputObj is String) {
            this.input = inputObj
        } else {
            throw IllegalArgumentException(
                "Invalid stonecutting recipe dictionary: input must be a string"
            )
        }

        val resultObj = LegacyKeys.lookup(dict, "Result")
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException(
                "Invalid stonecutting recipe dictionary: result must be a string"
            )
        }

        return this
    }

    /** Converts this definition into a Bukkit [StonecuttingRecipe]. */
    override fun toRecipe(baseKey: NamespacedKey?): Recipe {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val inputDef =
            input
                ?: throw IllegalArgumentException("Invalid stonecutting recipe: input must be set")
        val resultDef =
            result
                ?: throw IllegalArgumentException("Invalid stonecutting recipe: result must be set")
        val out = ItemUtil.itemstackFromString(resultDef).first!!
        val `in` = recipeChoice(inputDef)
        return StonecuttingRecipe(key(bk), out, `in`)
    }
}
