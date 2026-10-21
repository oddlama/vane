package org.oddlama.vane.core.config.recipes

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice.MaterialChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.oddlama.vane.util.ItemUtil
import org.oddlama.vane.util.LegacyKeys

/**
 * Recipe definition for smithing transform recipes.
 *
 * @param name logical recipe name.
 */
class SmithingRecipeDefinition(name: String?) : RecipeDefinition(name) {
    /** Base ingredient definition. */
    private var base: String? = null

    /** Addition ingredient definition. */
    private var addition: String? = null

    /** Whether smithing should copy NBT from base item to result. */
    private var copyNbt = false

    /** Result item definition string. */
    private var result: String? = null

    /** Sets base ingredient definition. */
    fun base(base: String?): SmithingRecipeDefinition {
        this.base = base
        return this
    }

    /** Sets base ingredient from a Bukkit tag. */
    fun base(tag: Tag<*>): SmithingRecipeDefinition {
        return base("#" + tag.key())
    }

    /** Sets base ingredient from a material key. */
    fun base(material: Material): SmithingRecipeDefinition {
        return base(material.key().toString())
    }

    /** Sets addition ingredient definition. */
    fun addition(addition: String?): SmithingRecipeDefinition {
        this.addition = addition
        return this
    }

    /** Sets whether NBT should be copied from base input. */
    fun copyNbt(copyNbt: Boolean): SmithingRecipeDefinition {
        this.copyNbt = copyNbt
        return this
    }

    /** Sets addition ingredient from a Bukkit tag. */
    fun addition(tag: Tag<*>): SmithingRecipeDefinition {
        return addition("#" + tag.key())
    }

    /** Sets addition ingredient from a material key. */
    fun addition(material: Material): SmithingRecipeDefinition {
        return addition(material.key().toString())
    }

    /** Sets result item definition string. */
    fun result(result: String?): SmithingRecipeDefinition {
        this.result = result
        return this
    }

    /** Serializes this recipe definition to dictionary form. */
    override fun toDict(): Any {
        val dict = mutableMapOf<String?, Any?>()
        dict["Base"] = this.base
        dict["Addition"] = this.addition
        dict["CopyNbt"] = this.copyNbt
        dict["Result"] = this.result
        dict["Type"] = "smithing"
        return dict
    }

    /** Loads this recipe definition from dictionary form. */
    override fun fromDict(dict: Any?): RecipeDefinition {
        require(dict is MutableMap<*, *>) {
            "Invalid smithing recipe dictionary: Argument must be a Map<String, Object>!"
        }
        val baseObj = LegacyKeys.lookup(dict, "Base")
        if (baseObj is String) {
            this.base = baseObj
        } else {
            throw IllegalArgumentException(
                "Invalid smithing recipe dictionary: base must be a string"
            )
        }

        val additionObj = LegacyKeys.lookup(dict, "Addition")
        if (additionObj is String) {
            this.addition = additionObj
        } else {
            throw IllegalArgumentException(
                "Invalid smithing recipe dictionary: addition must be a string"
            )
        }

        val copyNbtObj = LegacyKeys.lookup(dict, "CopyNbt")
        if (copyNbtObj is Boolean) {
            this.copyNbt = copyNbtObj
        } else {
            throw IllegalArgumentException(
                "Invalid smithing recipe dictionary: copyNbt must be a bool"
            )
        }

        val resultObj = LegacyKeys.lookup(dict, "Result")
        if (resultObj is String) {
            this.result = resultObj
        } else {
            throw IllegalArgumentException(
                "Invalid smithing recipe dictionary: result must be a string"
            )
        }

        return this
    }

    /** Converts this definition into a Bukkit [SmithingTransformRecipe]. */
    override fun toRecipe(baseKey: NamespacedKey?): Recipe {
        val bk = baseKey ?: throw IllegalArgumentException("baseKey cannot be null")
        val baseDef =
            base ?: throw IllegalArgumentException("Invalid smithing recipe: base must be set")
        val additionDef =
            addition
                ?: throw IllegalArgumentException("Invalid smithing recipe: addition must be set")
        val resultDef =
            result ?: throw IllegalArgumentException("Invalid smithing recipe: result must be set")

        return SmithingTransformRecipe(
            key(bk),
            ItemUtil.itemstackFromString(resultDef).first!!,
            MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            recipeChoice(baseDef),
            recipeChoice(additionDef),
            copyNbt,
        )
    }
}
