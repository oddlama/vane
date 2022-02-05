package org.oddlama.vane.trifles;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.oddlama.vane.core.Listener;

import io.papermc.paper.event.entity.EntityMoveEvent;

public class FastWalkingListener extends Listener<Trifles> {

	FastWalkingGroup fast_walking;

	public FastWalkingListener(FastWalkingGroup context) {
		super(context);
		this.fast_walking = context;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_move(final PlayerMoveEvent event) {
		// Players mustn't be flying
		final var player = event.getPlayer();
		if (player.isGliding()) {
			return;
		}

		LivingEntity effect_entity = player;
		if (player.isInsideVehicle() && player.getVehicle() instanceof LivingEntity vehicle) {
			effect_entity = vehicle;
		}

		// Inspect block type just a little below
		var block = effect_entity.getLocation().clone().subtract(0.0, 0.1, 0.0).getBlock();
		if (!fast_walking.config_materials.contains(block.getType())) {
			return;
		}

		// Apply potion effect
		effect_entity.addPotionEffect(fast_walking.walk_speed_effect);
	}

	// This is fired for entities except players.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_entity_move(final EntityMoveEvent event) {
		final var entity = event.getEntity();

		// Inspect block type just a little below
		var block = event.getTo().clone().subtract(0.0, 0.1, 0.0).getBlock();
		if (!fast_walking.config_materials.contains(block.getType())) {
			return;
		}

		// Apply potion effect
		entity.addPotionEffect(fast_walking.walk_speed_effect);
	}
}
