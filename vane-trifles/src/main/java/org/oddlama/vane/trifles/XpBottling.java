package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.MaterialUtil.is_seeded_plant;
import static org.oddlama.vane.util.PlayerUtil.harvest_plant;

import java.util.Arrays;
import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.oddlama.vane.annotation.config.ConfigInt;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class XpBottling extends Listener<Trifles> {
	@ConfigInt(def = 10, min = 1, desc = "Amount of levels to store when using an iron nugget.")
	public int config_iron_nugget_xp;
	@ConfigInt(def = 30, min = 1, desc = "Amount of levels to store when using a gold nugget.")
	public int config_gold_nugget_xp;

	public XpBottling(Context<Trifles> context) {
		super(context.group("xp_bottling", "Enables bottling experience by combining a Bottle o' Enchanting with a iron or gold nugget in an anvil."));
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void on_prepare_anvil(final PrepareAnvilEvent event) {
		if (event.getResult() != null) {
			return;
		}

		final var inventory = event.getInventory();
		final var item1 = inventory.getFirstItem();
		if (item1 == null || item1.getType() != Material.EXPERIENCE_BOTTLE) {
			return;
		}

		final var item2 = inventory.getSecondItem();
		if (item2 == null) {
			return;
		}

		final int levels_to_store;
		switch (item2.getType()) {
			default: return;
			case IRON_NUGGET: levels_to_store = 10; break;
			case GOLD_NUGGET: levels_to_store = 30; break;
		}

		inventory.setMaximumRepairCost(levels_to_store);
		inventory.setRepairCost(levels_to_store);

		// TODO would consume whole second stack...
		// custom item(s) drinkable :DD
		// TODO custom item(s) drinkable :DD
		final var item = new ItemStack(Material.EXPERIENCE_BOTTLE);
		final var meta = item.getItemMeta();
		meta.setLore(Arrays.asList(new String[] { "" }));
		event.setResult(item);
	}
}
