package org.oddlama.vane.trifles.items.storage

import java.util.*
import org.bukkit.Material
import org.bukkit.Tag
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.SmithingRecipeDefinition
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles

@VaneItem(name = "backpack", base = Material.SHULKER_BOX, modelData = 0x760017, version = 1)
/** Wearable-style storage item based on a shulker box. */
class Backpack(context: Context<Trifles?>) : StorageItem(context) {
    /** Defines the backpack smithing recipe. */
    override fun defaultRecipes(): RecipeList =
        RecipeList.of(
            SmithingRecipeDefinition("from_shulker_box")
                .base(Tag.SHULKER_BOXES)
                .addition(Material.LEATHER_CHESTPLATE)
                .copyNbt(true)
                .result(key().toString())
        )

    /** Disables conflicting vanilla behaviors for backpack items. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> =
        EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.DISPENSE)
}
