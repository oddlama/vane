package org.oddlama.vane.trifles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class WalkSpeedListener implements Listener {
	Trifles trifles;

	public WalkSpeedListener(Trifles trifles) {
		this.trifles = trifles;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_move(final PlayerMoveEvent event) {
		// Inspect block type just a little below the player
		var block = event.getTo().clone().subtract(0.0, 0.1, 0.0).getBlock();
		if (!trifles.config_fast_walking_materials.contains(block.getType())) {
			return;
		}

		// Apply potion effect
		event.getPlayer().addPotionEffect(trifles.walk_speed_effect);
	}
}
