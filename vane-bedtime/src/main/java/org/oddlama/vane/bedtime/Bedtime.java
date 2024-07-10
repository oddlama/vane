package org.oddlama.vane.bedtime;

import static org.oddlama.vane.util.WorldUtil.change_time_smoothly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.util.Nms;

@VaneModule(name = "bedtime", bstats = 8639, config_version = 3, lang_version = 5, storage_version = 1)
public class Bedtime extends Module<Bedtime> {

	// One set of sleeping players per world, to keep track
	private HashMap<UUID, HashSet<UUID>> world_sleepers = new HashMap<>();

	// Configuration
	@ConfigDouble(
		def = 0.5,
		min = 0.0,
		max = 1.0,
		desc = "The percentage of sleeping players required to advance time."
	)
	double config_sleep_threshold;

	@ConfigLong(
		def = 1000,
		min = 0,
		max = 12000,
		desc = "The target time in ticks to advance to. 1000 is just after sunrise."
	)
	long config_target_time;

	@ConfigLong(def = 100, min = 0, max = 1200, desc = "The interpolation time in ticks for a smooth change of time.")
	long config_interpolation_ticks;

	// Language
	@LangMessage
	private TranslatedMessage lang_player_bed_enter;

	@LangMessage
	private TranslatedMessage lang_player_bed_leave;

	public BedtimeDynmapLayer dynmap_layer;
	public BedtimeBlueMapLayer blue_map_layer;

	public Bedtime() {
		dynmap_layer = new BedtimeDynmapLayer(this);
		blue_map_layer = new BedtimeBlueMapLayer(this);
	}

	public void start_check_world_task(final World world) {
		if (enough_players_sleeping(world)) {
			schedule_task(
				() -> {
					check_world_now(world);
					// Subtract two ticks so this runs one tick before minecraft would
					// advance time (if all players are asleep), which would effectively cancel the task.
				},
				100 - 2
			);
		}
	}

	public void check_world_now(final World world) {
		// Abort task if condition changed
		if (!enough_players_sleeping(world)) {
			return;
		}

		// Let the sun rise, and set weather
		change_time_smoothly(world, this, config_target_time, config_interpolation_ticks);
		world.setStorm(false);
		world.setThundering(false);

		// Clear sleepers
		reset_sleepers(world);

		// Wakeup players as if they were actually sleeping through the night
		world
			.getPlayers()
			.stream()
			.filter(Player::isSleeping)
			.forEach(p -> {
				// skipSleepTimer = false (-> set sleepCounter to 100)
				// updateSleepingPlayers = false
				Nms.get_player(p).stopSleepInBed(false, false);
			});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_bed_enter(PlayerBedEnterEvent event) {
		final var player = event.getPlayer();
		final var world = player.getWorld();

		// Update marker
		dynmap_layer.update_marker(player);
		blue_map_layer.update_marker(player);

		schedule_next_tick(() -> {
			// Register the new player as sleeping
			add_sleeping(world, player);
			// Start a sleep check task
			start_check_world_task(world);
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_bed_leave(PlayerBedLeaveEvent event) {
		remove_sleeping(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_quit(PlayerQuitEvent event) {
		// Start a sleep check task
		start_check_world_task(event.getPlayer().getWorld());
	}

	private static String percentage_str(double percentage) {
		return String.format("§6%.2f", 100.0 * percentage) + "%";
	}

	private long get_amount_sleeping(final World world) {
		//return world.getPlayers().stream()
		//	.filter(p -> p.getGameMode() != GameMode.SPECTATOR)
		//	.filter(p -> p.isSleeping())
		//	.count();

		final var world_id = world.getUID();
		var sleepers = world_sleepers.get(world_id);
		if (sleepers == null) {
			return 0;
		}
		return sleepers.size();
	}

	private long get_potential_sleepers_in_world(final World world) {
		return world.getPlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).count();
	}

	private double get_percentage_sleeping(final World world) {
		final var count_sleeping = get_amount_sleeping(world);
		if (count_sleeping == 0) {
			return 0.0;
		}

		return (double)count_sleeping / get_potential_sleepers_in_world(world);
	}

	private boolean enough_players_sleeping(final World world) {
		return get_percentage_sleeping(world) >= config_sleep_threshold;
	}

	private void add_sleeping(final World world, final Player player) {
		// Add player to sleepers
		final var world_id = world.getUID();
		var sleepers = world_sleepers.computeIfAbsent(world_id, k -> new HashSet<>());

		sleepers.add(player.getUniqueId());

		// Broadcast a sleeping message
		var percent = get_percentage_sleeping(world);
		var count_sleeping = get_amount_sleeping(world);
		var count_required = (int)Math.ceil(get_potential_sleepers_in_world(world) * config_sleep_threshold);
		lang_player_bed_enter.broadcast_world_action_bar(world,
				"§6" + player.getName(),
				"§6" + percentage_str(percent),
				String.valueOf(count_sleeping),
				String.valueOf(count_required),
				"§6" + world.getName());
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
			// Broadcast a sleeping message
			var percent = get_percentage_sleeping(world);
			var count_sleeping = get_amount_sleeping(world);
			var count_required = (int)Math.ceil(get_potential_sleepers_in_world(world) * config_sleep_threshold);
			lang_player_bed_leave.broadcast_world_action_bar(world,
					"§6" + player.getName(),
					"§6" + percentage_str(percent),
					String.valueOf(count_sleeping),
					String.valueOf(count_required),
					"§6" + world.getName());
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
