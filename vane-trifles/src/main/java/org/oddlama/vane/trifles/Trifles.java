package org.oddlama.vane.trifles;

import static org.oddlama.vane.util.WorldUtil.broadcast;
import static org.oddlama.vane.util.WorldUtil.change_time_smoothly;
import static org.oddlama.vane.util.Util.ms_to_ticks;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import org.oddlama.vane.annotation.ConfigBoolean;
import org.oddlama.vane.annotation.ConfigLong;
import org.oddlama.vane.annotation.ConfigMaterialSet;
import org.oddlama.vane.annotation.ConfigVersion;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.LangMessage;
import org.oddlama.vane.annotation.LangString;
import org.oddlama.vane.annotation.LangVersion;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.util.Nms;

@VaneModule
public class Trifles extends Module implements Listener {
	// Configuration
	@ConfigVersion(1)
	public long config_version;

	@ConfigBoolean(def = true, desc = "Enable faster walking on grass paths.")
	boolean config_enable_fast_walking;

	@ConfigLong(def = 2000, min = 50, max = 5000, desc = "Speed effect duration in milliseconds.")
	long config_speed_duration;

	@ConfigMaterialSet(def = {Material.GRASS_PATH}, desc = "Materials on which players will walk faster.")
	Set<Material> config_fast_walking_materials;

	// Language
	@LangVersion(1)
	public long lang_version;

	// Variables
	private PotionEffect effect;

	@Override
	public void onEnable() {
		super.onEnable();
		register_listener(this);
	}

	@Override
	protected void on_config_change() {
		effect = new PotionEffect(PotionEffectType.SPEED, (int)ms_to_ticks(config_speed_duration), 1)
			.withAmbient(false)
			.withParticles(false)
			.withIcon(false);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_move(final PlayerMoveEvent event) {
		if (!config_enable_fast_walking) {
			return;
		}

		// Inspect block type just a little below the player
		var block = event.getTo().clone().subtract(0.0, 0.1, 0.0).getBlock();
		if (!config_fast_walking_materials.contains(block.getType())) {
			return;
		}

		// Apply potion effect
		event.getPlayer().addPotionEffect(effect);
	}
}
