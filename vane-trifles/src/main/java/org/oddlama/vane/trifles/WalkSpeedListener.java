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

import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.util.Nms;

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
