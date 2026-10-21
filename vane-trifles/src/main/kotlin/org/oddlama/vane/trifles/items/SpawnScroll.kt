package org.oddlama.vane.trifles.items

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.trifles.commands.Setspawn

@VaneItem(
    name = "spawn_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 40,
    modelData = 0x760010,
    version = 1,
)
/** Teleports players to the configured global spawn world. */
class SpawnScroll(context: Context<Trifles?>) : Scroll(context, 6000) {
    /** Defines the spawn scroll crafting recipe. */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("ABA", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('A', Material.WHEAT_SEEDS)
                .setIngredient('B', Tag.SAPLINGS)
                .result(key().toString())
        )
    }

    /** Resolves the current spawn destination, preferring the tagged spawn world. */
    override fun teleportLocation(
        scroll: ItemStack?,
        player: Player?,
        imminentTeleport: Boolean,
    ): Location {
        val module = requireNotNull(module)
        val worlds = module.server.worlds
        val spawnWorld = worlds.firstOrNull { world ->
            world.persistentDataContainer.getOrDefault(
                Setspawn.IS_SPAWN_WORLD,
                PersistentDataType.INTEGER,
                0,
            ) == 1
        }

        // Fallback to the first loaded world if no spawn world marker exists.
        return (spawnWorld ?: worlds.first()).spawnLocation
    }
}
