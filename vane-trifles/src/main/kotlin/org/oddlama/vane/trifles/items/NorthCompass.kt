package org.oddlama.vane.trifles.items

import java.util.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CompassMeta
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.api.CustomItem
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles

@VaneItem(name = "north_compass", base = Material.COMPASS, modelData = 0x760013, version = 1)
/** Compass variant that consistently points north through a synthetic lodestone target. */
class NorthCompass(context: Context<Trifles?>) :
    org.oddlama.vane.core.item.CustomItem<Trifles?>(context) {
    /** Defines the north compass crafting recipe. */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape(" M ", "MRM", " M ")
                .setIngredient('M', Material.COPPER_INGOT)
                .setIngredient('R', Material.REDSTONE)
                .result(key().toString())
        )
    }

    /** Initializes lodestone metadata so the compass points north-like consistently. */
    override fun updateItemStack(itemStack: ItemStack): ItemStack {
        val module = module ?: return itemStack
        val worlds: MutableList<World> = module.server.worlds
        if (worlds.isNotEmpty()) {
            val world = worlds[0]
            itemStack.editMeta(CompassMeta::class.java) { meta ->
                meta.lodestone = Location(world, 0.0, 0.0, -300000000.0)
                meta.isLodestoneTracked = false
            }
        }
        return itemStack
    }

    /** Ensures existing north compass items keep valid lodestone metadata on interaction. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onPlayerClickInventory(event: InventoryClickEvent) {
        val item = event.currentItem
        if (item == null || item.type != Material.COMPASS) {
            return
        }

        val customItem: CustomItem? = module?.core?.itemRegistry()?.get(item)
        if (customItem !is NorthCompass || !customItem.enabled()) {
            return
        }

        // Refresh metadata here so externally-created instances remain valid.
        item.editMeta(CompassMeta::class.java) { meta ->
            // Keep world binding immutable after first initialization.
            if (!meta.hasLodestone()) {
                meta.isLodestoneTracked = false
                meta.lodestone = Location(event.whoClicked.world, 0.0, 0.0, -300000000.0)
            }
        }
    }

    /** Prevents conflicting vanilla interactions. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        return EnumSet.of(InhibitBehavior.USE_IN_VANILLA_RECIPE, InhibitBehavior.USE_OFFHAND)
    }
}
