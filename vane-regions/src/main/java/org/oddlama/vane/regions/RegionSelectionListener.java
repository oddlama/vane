package org.oddlama.vane.regions;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.Material;

import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.region.RoleSetting;

public class RegionSelectionListener extends Listener<Regions> {
	public RegionSelectionListener(Context<Regions> context) {
		super(context);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void on_player_interact(final PlayerInteractEvent event) {
		// Require main hand event
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		// Require empty hand
		if (event.getItem() != null) {
			return;
		}

		final var player = event.getPlayer();
		final var selection = get_module().get_region_selection(player);
		if (selection == null) {
			return;
		}

		if (player.getEquipment().getItemInMainHand().getType() != Material.AIR ||
			player.getEquipment().getItemInOffHand().getType() != Material.AIR) {
			return;
		}

		final var block = event.getClickedBlock();
		switch (event.getAction()) {
			default:
				return;

			case LEFT_CLICK_BLOCK:
				selection.primary = block;
				break;

			case RIGHT_CLICK_BLOCK:
				selection.secondary = block;
				break;
		}

		event.setUseInteractedBlock(Event.Result.DENY);
		event.setUseItemInHand(Event.Result.DENY);
	}
}
