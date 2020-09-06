package org.oddlama.vane.admin;

import static org.oddlama.vane.util.WorldUtil.broadcast;
import static org.oddlama.vane.util.Nms.set_air_no_drops;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

import org.bukkit.Material;
import org.bukkit.Effect;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.util.Vector;

import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.Message;

public class WorldRebuild extends Listener<Admin> {
	@ConfigLong(def = 2000, min = 0, desc = "Delay in milliseconds until the world will be rebuilt.")
	private long config_delay;
	@ConfigDouble(def = 1.5, min = 1.0, desc = "Rebuild speed gain factor. The delay until the next block will be: current_delay / speed_gain.")
	private double config_speed_gain;
	@ConfigLong(def = 50, min = 0, desc = "Minimum delay in milliseconds between rebuilding two blocks. Anything <= 50 milliseconds will be one tick.")
	private long config_min_delay;

	public WorldRebuild(Context<Admin> context) {
		super(context.group("world_rebuild", "Instead of cancelling explosions, the world will regenerate after a short amount of time."));
	}

	private ArrayList<Rebuilder> rebuilders = new ArrayList<>();

	public void rebuild(final List<Block> blocks) {
		// Store a snapshot of all block states
		final var states = new ArrayList<BlockState>();
		for (final var block : blocks) {
			states.add(block.getState());
		}

		// Set everything to air without triggering physics
		for (final var block : blocks) {
			set_air_no_drops(block);
		}

		// Schedule rebuild
		rebuilders.add(new Rebuilder(states));
	}

	@Override
	public void on_disable() {
		// Finish all pending rebuilds now!
		for (final var r : rebuilders) {
			r.finish_now();
		}
		rebuilders.clear();
	}

	public class Rebuilder implements Runnable {
		private List<BlockState> states;
		private BukkitTask task = null;
		private long delay = 0;

		public Rebuilder(final List<BlockState> states) {
			this.states = states;
			if (states.isEmpty()) {
				return;
			}

			// Find top center point for rebuild order reference
			Vector center = new Vector(0, 0, 0);
			int max_y = 0;
			for (final var state : this.states) {
				max_y = Math.max(max_y, state.getY());
				center.add(state.getLocation().toVector());
			}
			center.multiply(1.0 / states.size());
			center.setY(max_y + 1);

			// Sort blocks to rebuild them in a ordered fashion
			Collections.sort(this.states, new RebuildComparator(center));

			// Initialize delay
			delay = config_delay;
			task = get_module().schedule_task(this, delay / 50);
		}

		private void finish() {
			task = null;
			WorldRebuild.this.rebuilders.remove(this);
		}

		private void rebuild_next_block() {
			rebuild_block(states.remove(states.size() - 1));
		}

		private void rebuild_block(final BlockState state) {
			final var block = state.getBlock();

			// Break any block that isn't air first
			if (block.getType() != Material.AIR) {
				block.breakNaturally();
			}

			// Force update without physics
			state.update(true, false);
			// Second update forces block state specific update
			state.update(true, false);

			// Play sound
			state.getWorld().playEffect(state.getLocation(), Effect.STEP_SOUND, state.getType());
		}

		public void finish_now() {
			if (task != null) {
				task.cancel();
			}

			for (final var state : states) {
				rebuild_block(state);
			}

			finish();
		}

		@Override
		public void run() {
			if (states.isEmpty()) {
				finish();
			} else {
				// Rebuild next block
				rebuild_next_block();

				// Adjust delay
				delay = Math.max(config_min_delay, (int)(delay / config_speed_gain));
				WorldRebuild.this.get_module().schedule_task(this, delay / 50);
			}
		}
	}

	public static class RebuildComparator implements Comparator<BlockState> {
		private Vector reference_point;

		public RebuildComparator(final Vector reference_point) {
			this.reference_point = reference_point;
		}

		@Override
		public int compare(final BlockState a, final BlockState b) {
			// Sort by distance to top-most center. Last block will be rebuilt first.
			final var da = a.getLocation().toVector().subtract(reference_point).lengthSquared();
			final var db = b.getLocation().toVector().subtract(reference_point).lengthSquared();
			if (da < db) {
				return 1;
			} else if (db > da) {
				return -1;
			} else {
				return 0;
			}
		}
	}
}
