package org.oddlama.vane.trifles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class RepairCostLimiter extends Listener<Trifles> {

    @ConfigInt(
        def = 39,
        min = 0,
        desc = "Limit anvil crafting cost. Set < 40 to remove 'Too Expensive' altogether. (Costs greater than 40 will still be craftable, even if it shows 'Too Expensive')"
    )
    public int config_max_repair_cost;

    public RepairCostLimiter(Context<Trifles> context) {
        super(
            context.group(
                "repair_cost_limiter",
                "Removes the cost limit on the anvil for all recipes. This means even if the client shows 'Too Expensive' in the anvil, the result may still be crafted, as long as the player has the required amount of levels (which unfortunately will not be shown)."
            )
        );
    }

    // Set maximum item repair cost, if configured
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void on_prepare_anvil(final PrepareAnvilEvent event) {
        final var view = event.getView();
        view.setMaximumRepairCost(999999);
        if (view.getRepairCost() > config_max_repair_cost) {
            view.setRepairCost(config_max_repair_cost);
        }
    }
}
