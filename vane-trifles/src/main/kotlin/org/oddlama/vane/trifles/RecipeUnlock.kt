package org.oddlama.vane.trifles

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.Nms

/** Unlocks all known recipes for players when they join the server. */
class RecipeUnlock(context: Context<Trifles?>) :
    Listener<Trifles?>(context.group("RecipeUnlock", "Unlocks all recipes when a player joins.")) {
    /** Attempts to unlock every recipe for the joining player and logs the unlock count. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val count = Nms.unlockAllRecipes(player)
        if (count > 0) {
            module
                ?.log
                ?.info(
                    "Given $count recipes to " +
                        LegacyComponentSerializer.legacySection().serialize(player.displayName())
                )
        }
    }
}
