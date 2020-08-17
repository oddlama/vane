package org.oddlama.imex.bedtime;

import static org.oddlama.imex.util.WorldUtil.broadcast;
import static org.oddlama.imex.util.WorldUtil.change_time_smoothly;

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
import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.annotation.ImexModule;
import org.oddlama.imex.annotation.LangMessage;
import org.oddlama.imex.annotation.LangString;
import org.oddlama.imex.annotation.LangVersion;
import org.oddlama.imex.core.Module;
import org.oddlama.imex.util.Nms;

@ImexModule
public class Bedtime extends Module implements Listener {
	// One set of sleeping players per world, to keep track
	private HashMap<UUID, HashSet<UUID>> world_sleepers = new HashMap<>();

	// Configuration
	@ConfigVersion(1)
	public long config_version;

	@ConfigDouble(def = 0.5, min = 0.0, max = 1.0, desc = "The percentage of sleeping players required to advance time.")
	double config_sleep_threshold;
	@ConfigLong(def = 1000, min = 0, max = 12000, desc = "The target time in ticks to advance to. 1000 is just after sunrise.")
	long config_target_time;
	@ConfigLong(def = 100, min = 0, max = 1200, desc = "The interpolation time in ticks for a smooth change of time.")
	long config_interpolation_ticks;

	// Language
	@LangVersion(1)
	public long lang_version;

	@LangMessage()
	MessageFormat lang_player_bed_enter;
	@LangMessage()
	MessageFormat lang_player_bed_leave;
	@LangString()
	String lang_sleep_success;

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
					change_time_smoothly(world, this, config_target_time, config_interpolation_ticks);
					world.setStorm(false);
					world.setThundering(false);

					// Send message
					broadcast(world, lang_sleep_success);

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
		return get_percentage_sleeping(world) >= config_sleep_threshold;
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
		broadcast(world, lang_player_bed_enter.format(new Object[] {
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
			broadcast(world, lang_player_bed_leave.format(new Object[] {
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
