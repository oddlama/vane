package org.oddlama.vane.trifles

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.MaterialUtil
import org.oddlama.vane.util.PlayerUtil

/** Harvests and replants mature crops when players right-click with an empty hand. */
class HarvestListener(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group(
            "BetterHarvesting",
            "Enables better harvesting. Right clicking on grown crops with bare hands will then harvest the plant and also replant it.",
        )
    ) {
    /** Handles right-click crop harvesting for supported seeded plants. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerHarvest(event: PlayerInteractEvent) {
        if (
            !event.hasBlock() ||
                event.hand != EquipmentSlot.HAND ||
                event.action != Action.RIGHT_CLICK_BLOCK
        ) {
            return
        }

        // Only continue for seeded plant blocks.
        val clickedBlock = event.clickedBlock ?: return
        val type = clickedBlock.type
        if (!MaterialUtil.isSeededPlant(type)) {
            return
        }

        val player = event.player
        if (PlayerUtil.harvestPlant(player, clickedBlock)) {
            event.hand?.let { PlayerUtil.swingArm(player, it) }
        }
    }
}
