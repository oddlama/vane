package org.oddlama.vane.trifles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;

import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class FastWalkingListener extends Listener<Trifles> {
	FastWalkingGroup fast_walking;
	public FastWalkingListener(FastWalkingGroup context) {
		super(context);
		this.fast_walking = context;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_move(final PlayerMoveEvent event) {
		// Inspect block type just a little below the player
		var block = event.getTo().clone().subtract(0.0, 0.1, 0.0).getBlock();
		if (!fast_walking.config_materials.contains(block.getType())) {
			return;
		}

		// Apply potion effect
		event.getPlayer().addPotionEffect(fast_walking.walk_speed_effect);
	}
}
