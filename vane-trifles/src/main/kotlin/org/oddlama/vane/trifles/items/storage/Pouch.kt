package org.oddlama.vane.trifles.items.storage

import java.util.*
import org.bukkit.Material
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles

@VaneItem(name = "pouch", base = Material.DROPPER, modelData = 0x760016, version = 1)
/** Compact storage item variant with dropper base material. */
class Pouch(context: Context<Trifles?>) : StorageItem(context) {
    /** Defines the pouch crafting recipe. */
    override fun defaultRecipes(): RecipeList =
        RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("SLS", "L L", "LLL")
                .setIngredient('S', Material.STRING)
                .setIngredient('L', Material.RABBIT_HIDE)
                .result(key().toString())
        )

    /** Disables conflicting vanilla behaviors for pouch items. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> =
        EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE)
}
