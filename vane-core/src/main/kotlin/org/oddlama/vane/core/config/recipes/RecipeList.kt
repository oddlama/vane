package org.oddlama.vane.core.config.recipes

import org.oddlama.vane.core.config.ConfigDictSerializable
import org.oddlama.vane.core.config.ConfigEntryNames

/**
 * Config-serializable list wrapper for recipe definitions.
 *
 * @param recipes initial recipe definitions.
 */
class RecipeList(private var recipes: MutableList<RecipeDefinition?> = mutableListOf()) : ConfigDictSerializable {

    /** Returns the mutable list of recipe definitions. */
    fun recipes(): MutableList<RecipeDefinition?> = recipes

    /** Serializes this recipe list to dictionary form. */
    override fun toDict(): MutableMap<String, Any> =
        recipes.filterNotNull().associateTo(LinkedHashMap()) { r -> NAMES.toYaml(r.name!!) to (r.toDict() as Any) }

    /** Loads recipe definitions from dictionary form. */
    override fun fromDict(dict: MutableMap<String, Any>) {
        val names = NAMES.fromYaml(dict.keys, "recipes")
        recipes.clear()
        dict.values.zip(names).forEach { (value, name) -> recipes.add(RecipeDefinition.fromDict(name, value)) }
    }

    /**
     * Construction and name-mapping helpers.
     */
    companion object {
        /** Default recipes that are written under a PascalCase heading. */
        private val NAMES = ConfigEntryNames(
            mapOf(
                "generic" to "Generic",
                "terralith_generic" to "TerralithGeneric",
                "terralith_rare" to "TerralithRare",
                "ancientcity" to "AncientCity",
                "bastion" to "Bastion",
                "from_shulker_box" to "FromShulkerBox",
            )
        )

        @JvmStatic
                /** Creates a [RecipeList] from vararg definitions. */
        fun of(vararg defs: RecipeDefinition?): RecipeList = RecipeList(defs.toMutableList())
    }
}
