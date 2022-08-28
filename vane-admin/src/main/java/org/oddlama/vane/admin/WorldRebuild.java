package org.oddlama.vane.admin;

import static org.oddlama.vane.util.Nms.set_air_no_drops;
import static org.oddlama.vane.util.Conversions.ms_to_ticks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class WorldRebuild extends Listener<Admin> {

	@ConfigLong(def = 2000, min = 0, desc = "Delay in milliseconds until the world will be rebuilt.")
	private long config_delay;

	@ConfigDouble(
		def = 0.175,
		min = 0.0,
		desc = "Determines rebuild speed. Higher falloff means faster transition to quicker rebuild. After n blocks, the delay until the next block will be d_n = delay * exp(-x * delay_falloff). For example 0.0 will result in same delay for every block."
	)
	private double config_delay_falloff;

	@ConfigLong(
		def = 50,
		min = 50,
		desc = "Minimum delay in milliseconds between rebuilding two blocks. Anything <= 50 milliseconds will be one tick."
	)
	private long config_min_delay;

	public WorldRebuild(Context<Admin> context) {
		super(
			context.group(
				"world_rebuild",
				"Instead of cancelling explosions, the world will regenerate after a short amount of time."
			)
		);
	}

	private List<Rebuilder> rebuilders = new ArrayList<>();

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
		for (final var r : new ArrayList<>(rebuilders)) {
			r.finish_now();
		}
		rebuilders.clear();
	}

	public class Rebuilder implements Runnable {

		private List<BlockState> states;
		private BukkitTask task = null;
		private long amount_rebuild = 0;

		public Rebuilder(final List<BlockState> _states) {
			this.states = _states;
			if (this.states.isEmpty()) {
				return;
			}

			// Find top center point for rebuild order reference
			Vector center = new Vector(0, 0, 0);
			int max_y = 0;
			for (final var state : this.states) {
				max_y = Math.max(max_y, state.getY());
				center.add(state.getLocation().toVector());
			}
			center.multiply(1.0 / this.states.size());
			center.setY(max_y + 1);

			// Sort blocks to rebuild them in a ordered fashion
			this.states.sort(new RebuildComparator(center));

			// Initialize delay
			task = get_module().schedule_task(this, ms_to_ticks(config_delay));
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
			++amount_rebuild;

			// Break any block that isn't air first
			if (block.getType() != Material.AIR) {
				block.breakNaturally();
			}

			// Force update without physics to set block type
			state.update(true, false);
			// Second update forces block state specific update
			state.update(true, false);

			// Play sound
			block
				.getWorld()
				.playSound(
					block.getLocation(),
					block.getBlockSoundGroup().getPlaceSound(),
					SoundCategory.BLOCKS,
					1.0f,
					0.8f
				);
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
				final var delay = ms_to_ticks(
					Math.max(config_min_delay, (int) (config_delay * Math.exp(-amount_rebuild * config_delay_falloff)))
				);
				WorldRebuild.this.get_module().schedule_task(this, delay);
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
			return Double.compare(da, db);
		}
	}
}
