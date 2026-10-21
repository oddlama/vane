package org.oddlama.vane.trifles

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

/** Adjusts anvil repair costs to bypass or reduce the vanilla "Too Expensive" threshold. */
class RepairCostLimiter(context: Context<Trifles?>) :
    Listener<Trifles?>(
        context.group(
            "RepairCostLimiter",
            "Removes the cost limit on the anvil for all recipes. This means even if the client shows 'Too Expensive' in the anvil, the result may still be crafted, as long as the player has the required amount of levels (which unfortunately will not be shown).",
        )
    ) {
    /** Maximum repair cost shown/applied by anvil views. */
    @ConfigInt(
        def = 39,
        min = 0,
        desc =
            "Limit anvil crafting cost. Set < 40 to remove 'Too Expensive' altogether. (Costs greater than 40 will still be craftable, even if it shows 'Too Expensive')",
    )
    /** Maximum repair cost shown/applied by anvil views. */
    var configMaxRepairCost: Int = 0

    /** Applies the configured repair cap to the anvil result view. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val view = event.view
        view.maximumRepairCost = 999999
        view.repairCost = view.repairCost.coerceAtMost(configMaxRepairCost)
    }
}
