package org.oddlama.vane.trifles.items

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.inventory.ItemStack
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.trifles.event.PlayerTeleportScrollEvent
import org.oddlama.vane.util.StorageUtil

@VaneItem(
    name = "unstable_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 25,
    modelData = 0x760001,
    version = 1,
)
/** Teleports players back to their previous scroll-teleport origin. */
class UnstableScroll(context: Context<Trifles?>) : Scroll(context, 6000) {
    /** Action-bar message shown when no previous scroll teleport is recorded. */
    @LangMessage var langTeleportNoPreviousTeleport: TranslatedMessage? = null

    /** Defines the unstable scroll crafting recipe. */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("ABA", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.CHORUS_FRUIT)
                .setIngredient('A', Material.AMETHYST_SHARD)
                .setIngredient('B', Material.COMPASS)
                .result(key().toString())
        )
    }

    /** Returns the previously stored scroll teleport origin. */
    override fun teleportLocation(
        scroll: ItemStack?,
        player: Player?,
        imminentTeleport: Boolean,
    ): Location? {
        val p = player ?: return null
        val loc =
            StorageUtil.storageGetLocation(
                p.persistentDataContainer,
                LAST_SCROLL_TELEPORT_LOCATION,
                null,
            )
        if (imminentTeleport && loc == null) {
            langTeleportNoPreviousTeleport?.sendActionBar(p)
        }
        return loc
    }

    /** Stores the origin of each successful scroll teleport for later unstable-scroll use. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleportScroll(event: PlayerTeleportScrollEvent) {
        StorageUtil.storageSetLocation(
            event.player.persistentDataContainer,
            LAST_SCROLL_TELEPORT_LOCATION,
            event.from,
        )
    }

    companion object {
        /** Persistent key storing the last scroll teleport origin location. */
        val LAST_SCROLL_TELEPORT_LOCATION: NamespacedKey =
            StorageUtil.namespacedKey(
                "vane",
                "last_scroll_teleport_location",
            )
    }
}
