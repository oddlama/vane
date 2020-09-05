package org.oddlama.vane.trifles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.meta.Repairable;

import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class RepairCostLimiter extends Listener<Trifles> {
	@ConfigInt(def = 19, min = 0, desc = "Repair cost override for anvil-crafted items. Set < 39 to remove too-expensive for <item> + <material> crafting. Set < 20 to remove too-expensive for any item + item combination.")
	public int config_max_repair_cost;

	public RepairCostLimiter(Context<Trifles> context) {
		super(context.group("repair_cost_limiter", "Enables limited repair cost for all items. Can be used to remove too-expensive repairs."));
	}

	// Set maximum item repair cost, if configured
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_prepare_anvil(final PrepareAnvilEvent event) {
		final var item = event.getResult();
		if (item == null) {
			return;
		}

		final var meta = item.getItemMeta();
		if (meta == null || !(meta instanceof Repairable)) {
			return;
		}

		final var repairable = (Repairable)meta;
		if (repairable.getRepairCost() <= config_max_repair_cost) {
			return;
		}

		// Limit resulting item repair cost
		repairable.setRepairCost(config_max_repair_cost);
		item.setItemMeta(meta);
		event.setResult(item);
	}
}
