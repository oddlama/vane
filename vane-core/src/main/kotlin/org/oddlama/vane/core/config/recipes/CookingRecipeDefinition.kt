package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.*
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.LegacyKeys

/**
 * Recipe definition for furnace-like cooking recipes.
 *
 * @param name logical recipe name.
 * @param type cooking recipe type (`blasting`, `furnace`, `campfire`, `smoking`).
 */
class CookingRecipeDefinition(name: String?, private val type: String) : RecipeDefinition(name) {
    /** Input ingredient definition. */
    private var input: String? = null

    /** Result item definition. */
    private var result: String? = null

    /** Experience awarded by the recipe. */
    private var experience = 0.0f

    /** Cooking time in ticks. */
    private var cookingTime = 10

    init {
        when (this.type) {
            "blasting",
            "furnace",
            "campfire",
            "smoking" -> {}
            else ->
                throw IllegalArgumentException(
                    "Invalid cooking recipe: Unknown type '" + this.type + "'"
                )
        }
    }

    /** Sets input ingredient definition. */
    fun input(input: String?): CookingRecipeDefinition {
        this.input = input
        return this
    }

    /** Sets input ingredient by Bukkit tag. */
    fun input(tag: Tag<*>): CookingRecipeDefinition {
        return input("#" + tag.key())
    }

    /** Sets input ingredient by material key. */
    fun input(material: Material): CookingRecipeDefinition {
        return input(material.key().toString())
    }

    /** Sets result item definition. */
    fun result(result: String?): CookingRecipeDefinition {
        this.result = result
        return this
    }

    /** Serializes this recipe definition to dictionary form. */
    override fun toDict(): Any {
        val dict = mutableMapOf<String?, Any?>()
        dict["CookingTime"] = this.cookingTime
        dict["Experience"] = this.experience
        dict["Input"] = this.input
        dict["Result"] = this.result
        dict["Type"] = type
        return dict
    }

    /** Loads this recipe definition from dictionary form. */
    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) {
            "Invalid $type recipe dictionary: Argument must be a Map<String, Object>!"
        }
        val inputObj = LegacyKeys.lookup(dict, "Input")
        if (inputObj is String) {
            this.input = inputObj
        } else {
            throw IllegalArgumentException(
                "Invalid $type recipe dictionary: input must be a string"
            )
        }

        val resultObj = LegacyKeys.lookup(dict, "Result")
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException(
                "Invalid $type recipe dictionary: result must be a string"
            )
        }

        val experienceObj = LegacyKeys.lookup(dict, "Experience")
        if (experienceObj is Float) {
            this.experience = experienceObj
        } else {
            throw IllegalArgumentException(
                "Invalid $type recipe dictionary: experience must be a float"
            )
        }

        val cookingTimeObj = LegacyKeys.lookup(dict, "CookingTime")
        if (cookingTimeObj is Int) {
            this.cookingTime = cookingTimeObj
        } else {
            throw IllegalArgumentException(
                "Invalid $type recipe dictionary: cookingTime must be a int"
            )
        }

        return this
    }

    /** Converts this definition into a concrete Bukkit cooking recipe. */
    override fun toRecipe(baseKey: NamespacedKey?): Recipe? {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val out = ItemUtil.itemstackFromString(this.result!!).first!!
        val `in` = recipeChoice(input!!)
        return when (this.type) {
            "blasting" -> BlastingRecipe(key(bk), out, `in`, experience, cookingTime)
            "furnace" -> FurnaceRecipe(key(bk), out, `in`, experience, cookingTime)
            "campfire" -> CampfireRecipe(key(bk), out, `in`, experience, cookingTime)
            "smoking" -> SmokingRecipe(key(bk), out, `in`, experience, cookingTime)
            else ->
                throw IllegalArgumentException(
                    "Invalid cooking recipe: Unknown type '" + this.type + "'"
                )
        }
    }
}
