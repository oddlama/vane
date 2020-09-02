package org.oddlama.vane.admin;

import static org.oddlama.vane.util.WorldUtil.broadcast;

import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;

import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigStringList;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.Message;

public class HazardProtection extends Listener<Admin> {
	@ConfigBoolean(def = true, desc = "Restrict wither spawning to a list of worlds defined by wither_world_whitelist.")
	private boolean config_enable_wither_world_whitelist;
	@ConfigStringList(def = {"world_nether", "world_the_end"}, desc = "A list of worlds in which the wither may be spawned.")
	private List<String> config_wither_world_whitelist;
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

	@LangMessage
	private Message lang_wither_spawn_prohibited;

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
	public void on_block_ignite(final BlockIgniteEvent event) {
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

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_creature_spawn(final CreatureSpawnEvent event) {
		if (!config_enable_wither_world_whitelist) {
			return;
		}

		// Only for wither spawns
		if (event.getEntity().getType() != EntityType.WITHER) {
			return;
		}

		// Check if world is whitelisted
		final var world = event.getEntity().getWorld();
		if (config_wither_world_whitelist.contains(world.getName())) {
			return;
		}

		broadcast(world, lang_wither_spawn_prohibited.format(world.getName()));
		event.setCancelled(true);
	}
}
