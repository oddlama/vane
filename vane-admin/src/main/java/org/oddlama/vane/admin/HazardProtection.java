package org.oddlama.vane.admin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.annotation.config.ConfigBoolean;

public class HazardProtection extends Listener<Admin> {
	@ConfigBoolean(def = true, desc = "Disables explosions from the wither.")
	private boolean config_disable_wither_explosions;
	@ConfigBoolean(def = true, desc = "Disables explosions from creepers.")
	private boolean config_disable_creeper_explosions;
	@ConfigBoolean(def = true, desc = "Disables enderman block pickup.")
	private boolean config_disable_enderman_block_pickup;
	@ConfigBoolean(def = true, desc = "Disables entities from breaking doors (various zombies).")
	private boolean config_disable_door_breaking;
	@ConfigBoolean(def = true, desc = "Disables fire from lightning.")
	private boolean config_disable_lightning_fire;

	public HazardProtection(Context<Admin> context) {
		super(context.group("hazard_protection", "Enable hazard protection. The options below allow more fine-grained control over the hazards to protect from."));
	}

	private boolean disable_explosion(EntityType type) {
		switch (type) {
			default:
				return false;
			case WITHER:
			case WITHER_SKULL:
				 return config_disable_wither_explosions;
			case CREEPER:
				 return config_disable_creeper_explosions;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_entity_explode(final EntityExplodeEvent event) {
		if (event.getEntity() == null) {
			return;
		}

		if (disable_explosion(event.getEntityType())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_hanging_break_by_entity(final HangingBreakByEntityEvent event) {
		if (disable_explosion(event.getRemover().getType())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_entity_break_door(final EntityBreakDoorEvent event) {
		if (config_disable_door_breaking) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockIgnite(final BlockIgniteEvent event) {
		switch (event.getCause()) {
			default:
				return;

			case LIGHTNING:
				if (config_disable_lightning_fire) {
					event.setCancelled(true);
				}
				return;
		}
	}
}
