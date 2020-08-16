package org.oddlama.imex.bedtime;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;

import org.oddlama.imex.annotation.ConfigDouble;
import org.oddlama.imex.annotation.ConfigLong;
import org.oddlama.imex.annotation.ConfigString;
import org.oddlama.imex.annotation.ImexModule;
import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.annotation.LangString;
import org.oddlama.imex.core.Module;
import org.oddlama.imex.util.Nms;
import org.oddlama.imex.util.WorldUtil;

//// Basic
//@ConfigVersion(1)
//@ConfigLong(name = "version", def = 1, desc = "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated.")
//@ConfigString(name = "lang", def = "inherit", desc = "The language for this module. Specifying 'inherit' will use the value set for imex-core.")
//
//// Configuration
//@ConfigDouble(name = "sleep_threshold", def = 0.5, min = 0.0, max = 1.0, desc = "The percentage of sleeping players required to advance time")
//@ConfigLong(name = "target_time", def = 1000, min = 0, max = 12000, desc = "The target time in ticks [0-12000] to advance to. 1000 is just after sunrise.")
//@ConfigLong(name = "interpolation_ticks", def = 100, min = 0, max = 1200, desc = "The interpolation time in ticks for a smooth change of time.")
//
//// Language
//@LangVersion(1)
//@LangMessage(name = "player_bed_enter")
//@LangMessage(name = "player_bed_leave")
//@LangString(name = "sleep_success")

@ImexModule
public class Bedtime extends Module implements Listener {
	//public Config config;
	//public Lang lang;

	//@Override
	//public org.oddlama.imex.core.Config get_config() {
	//	return config;
	//}

	//@Override
	//public org.oddlama.imex.core.Lang get_lang() {
	//	return lang;
	//}

	// One set of sleeping players per world, to keep track
	private HashMap<UUID, HashSet<UUID>> world_sleepers = new HashMap<>();

//double config.sleep_threshold = 0.5;
//long config.target_time = 1000;
//long config.interpolation_ticks = 100;
//MessageFormat lang.player_bed_enter = new MessageFormat("A");
//MessageFormat lang.player_bed_leave = new MessageFormat("B");
//String lang.sleep_success = "C";

	@Override
	public void onEnable() {
		super.onEnable();
		register_listener(this);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_bed_enter(PlayerBedEnterEvent event) {
		final var player = event.getPlayer();
		final var world = player.getWorld();

		schedule_next_tick(() -> {
			// Register the new player as sleeping
			add_sleeping(world, player);

			if (enough_players_sleeping(world)) {
				schedule_task(() -> {
					// Abort task if condition changed
					if (!enough_players_sleeping(world)) {
						return;
					}

					// Let the sun rise, and set weather
					WorldUtil.change_time_smoothly(world, this, config.target_time, config.interpolation_ticks);
					world.setStorm(false);
					world.setThundering(false);

					// Send message
					WorldUtil.broadcast(world, lang.sleep_success);

					// Clear sleepers
					reset_sleepers(world);

					// Wakeup players as if they were actually sleeping through the night
					world.getPlayers().stream().filter(p -> p.isSleeping()).forEach(p -> {
						// flag0 false = set ticks sleeping to 100, flag1 true = recalculate world.everyoneSleeping
						Nms.getPlayer(player).wakeup(false, false);
					});

				// Subtract two ticks so this runs one tick before minecraft would
				// advance time (if all players are asleep), which would effectively cancel the task.
				}, 100 - 2);
			}
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerBedLeave(PlayerBedLeaveEvent event) {
		remove_sleeping(event.getPlayer());
	}

	private long get_amount_sleeping(final World world) {
		return world.getPlayers().stream()
			.filter(p -> p.getGameMode() != GameMode.SPECTATOR)
			.filter(p -> p.isSleeping())
			.count();
	}

	private double get_percentage_sleeping(final World world) {
		final var count_sleeping = get_amount_sleeping(world);
		if (count_sleeping == 0) {
			return 0.0;
		}

		return (double)count_sleeping / world.getPlayers().size();
	}

	private boolean enough_players_sleeping(final World world) {
		return get_percentage_sleeping(world) >= config.sleep_threshold;
	}

	private void add_sleeping(final World world, final Player player) {
		// Add player to sleepers
		final var world_id = world.getUID();
		var sleepers = world_sleepers.get(world_id);
		if (sleepers == null) {
			sleepers = new HashSet<UUID>();
			world_sleepers.put(world_id, sleepers);
		}

		sleepers.add(player.getUniqueId());

		// Broadcast sleeping message
		var percent = get_percentage_sleeping(world);
		WorldUtil.broadcast(world, lang.player_bed_enter.format(new Object[] {
		                               player.getName(),
		                               100.0 * percent}));
	}

	private void remove_sleeping(Player player) {
		final var world = player.getWorld();
		final var world_id = world.getUID();

		// Remove player from sleepers
		final var sleepers = world_sleepers.get(world_id);
		if (sleepers == null) {
			// No sleepers in this world. Abort.
			return;
		}

		if (sleepers.remove(player.getUniqueId())) {
			// Broadcast sleeping message
			var percent = get_percentage_sleeping(world);
			WorldUtil.broadcast(world, lang.player_bed_leave.format(new Object[] {
			                               player.getName(),
			                               100.0 * percent}));
		}
	}

	private void reset_sleepers(World world) {
		final var world_id = world.getUID();
		final var sleepers = world_sleepers.get(world_id);
		if (sleepers == null) {
			return;
		}

		sleepers.clear();
	}
}
