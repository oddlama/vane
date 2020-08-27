package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.BlockUtil.harvest_plant;
import static org.oddlama.vane.util.MaterialUtil.is_seed;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class HarvestListener extends Listener<Trifles> {
	public HarvestListener(Context<Trifles> context) {
		super(context.group("better_harvesting", "Enables better harvesting. Right clicking on grown crops with bare hands will then harvest the plant and also replant it."));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_harvest(PlayerInteractEvent event) {
		if (!event.hasBlock() || event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		// Only harvest when right clicking some plant type
		final var type = event.getClickedBlock().getType();
		if (!is_seed(type)) {
			return;
		}

		// Only when main hand is empty
		final var player = event.getPlayer();
		if (player.getEquipment().getItemInMainHand().getType() != Material.AIR) {
			return;
		}

		harvest_plant(event.getClickedBlock());
	}
}
