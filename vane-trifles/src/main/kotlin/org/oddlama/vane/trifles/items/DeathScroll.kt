package org.oddlama.vane.trifles.items

import java.util.*
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.oddlama.vane.annotation.item.VaneItem
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.config.recipes.RecipeList
import org.oddlama.vane.core.config.recipes.ShapedRecipeDefinition
import org.oddlama.vane.core.item.api.InhibitBehavior
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.trifles.Trifles
import org.oddlama.vane.util.StorageUtil

@VaneItem(
    name = "death_scroll",
    base = Material.WARPED_FUNGUS_ON_A_STICK,
    durability = 2,
    modelData = 0x760012,
    version = 1,
)
/** Teleports a player to their most recent death location within a time window. */
class DeathScroll(context: Context<Trifles?>) : Scroll(context, 6000) {
    /** Action-bar message shown when no valid recent death location is available. */
    @LangMessage var langTeleportNoRecentDeath: TranslatedMessage? = null

    /** Defines the death scroll crafting recipe. */
    override fun defaultRecipes(): RecipeList {
        return RecipeList.of(
            ShapedRecipeDefinition("generic")
                .shape("ABA", "EPE")
                .setIngredient('P', "vane_trifles:papyrus_scroll")
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('A', Material.BONE)
                .setIngredient('B', Material.RECOVERY_COMPASS)
                .result(key().toString())
        )
    }

    /** Adds additional behavior inhibition to prevent unsafe enchanting interactions. */
    override fun inhibitedBehaviors(): EnumSet<InhibitBehavior> {
        val set: EnumSet<InhibitBehavior> = super.inhibitedBehaviors()
        // Explicitly disallow adding new enchants.
        set.add(InhibitBehavior.NEW_ENCHANTS)
        return set
    }

    /** Resolves and validates the recent death teleport location. */
    override fun teleportLocation(
        scroll: ItemStack?,
        player: Player?,
        imminentTeleport: Boolean,
    ): Location? {
        val p = player ?: return null
        val pdc = p.persistentDataContainer
        val time = pdc.getOrDefault(RECENT_DEATH_TIME, PersistentDataType.LONG, 0L)
        var loc =
            StorageUtil.storageGetLocation(
                player.persistentDataContainer,
                RECENT_DEATH_LOCATION,
                null,
            )

        // Only keep death locations for 20 minutes.
        if (System.currentTimeMillis() - time > 20 * 60 * 1000L) {
            loc = null
        }

        if (imminentTeleport) {
            if (loc == null) {
                langTeleportNoRecentDeath?.sendActionBar(p)
            } else {
                // Consume saved destination after successful imminent check.
                pdc.remove(RECENT_DEATH_TIME)
                StorageUtil.storageRemoveLocation(pdc, RECENT_DEATH_LOCATION)
            }
        }

        return loc
    }

    /** Stores player death location and timestamp on every death event. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val pdc = event.player.persistentDataContainer
        StorageUtil.storageSetLocation(pdc, RECENT_DEATH_LOCATION, event.player.location)
        pdc.set(RECENT_DEATH_TIME, PersistentDataType.LONG, System.currentTimeMillis())
        event.player.setCooldown(this.baseMaterial(), 0)
    }

    companion object {
        /** Persistent key storing the recent death location. */
        val RECENT_DEATH_LOCATION: NamespacedKey =
            StorageUtil.namespacedKey(
                "vane",
                "recent_death_location",
            )

        /** Persistent key storing the timestamp of the recent death. */
        val RECENT_DEATH_TIME: NamespacedKey =
            StorageUtil.namespacedKey("vane", "recent_death_time")
    }
}
