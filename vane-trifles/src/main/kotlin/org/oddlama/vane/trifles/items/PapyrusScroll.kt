package org.oddlama.vane.trifles.items

import java.util.*
import org.bukkit.Material
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles

@VaneItem(name = "papyrus_scroll", base = Material.PAPER, modelData = 0x76000f, version = 1)
/** Crafting base item used by teleport scroll recipes. */
class PapyrusScroll(context: Context<Trifles?>) : CustomItem<Trifles?>(context) {
    /** Defines the papyrus scroll crafting recipe. */
    override fun defaultRecipes(): RecipeList =
        RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("RPP", "PEP", "PPG")
                .setIngredient('P', Material.PAPER)
                .setIngredient('R', Material.RABBIT_HIDE)
                .setIngredient('E', Material.ECHO_SHARD)
                .setIngredient('G', Material.GOLD_NUGGET)
                .result(key().toString())
        )

    /** Prevents papyrus scroll usage in unintended vanilla behaviors. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> =
        EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE)
}
