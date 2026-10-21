package org.oddlama.vane.trifles.items

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles

@VaneItem(
    name = "home_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 25,
    modelData = 0x760000,
    version = 1,
)
/** Teleports players to their personal respawn location. */
class HomeScroll(context: Context<Trifles?>) : Scroll(context, 10000) {
    /** Defines the home scroll crafting recipe. */
    override fun defaultRecipes(): RecipeList =
        RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("ABC", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('A', Material.CAMPFIRE)
                .setIngredient('B', Material.GOAT_HORN)
                .setIngredient('C', Tag.BEDS)
                .result(key().toString())
        )

    /** Resolves the player's respawn position as teleport destination. */
    override fun teleportLocation(
        scroll: ItemStack?,
        player: Player?,
        imminentTeleport: Boolean,
    ): Location? {
        val p = player ?: return null
        val toLocation = p.respawnLocation
        if (imminentTeleport && toLocation == null) {
            // `respawnLocation` is null when no valid respawn point exists.
            p.sendActionBar(
                Component.translatable("advancements.adventure.sleep_in_bed.description")
            )
        }
        return toLocation
    }
}
