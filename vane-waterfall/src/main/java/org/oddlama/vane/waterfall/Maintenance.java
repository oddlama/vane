package org.oddlama.vane.waterfall;

import static org.oddlama.vane.waterfall.Util.format_time;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class Maintenance {
	public static long SHUTDOWN_THRESHOLD = 10000l; // MESSAGE_SHUTDOWN if <= 10 seconds
	public static final long[] NOTIFY_TIMES = {
		240 * 60000l,
		180 * 60000l,
		120 * 60000l,
		60 * 60000l,
		30 * 60000l,
		15 * 60000l,
		10 * 60000l,
		5 * 60000l,
		4 * 60000l,
		3 * 60000l,
		2 * 60000l,
		60000l,
		30000l,
		10000l,
		5000l,
		4000l,
		3000l,
		2000l,
		1000l,
	};

	public static String MESSAGE_ABORTED =
	    "§7> §cServerwartung §l§6ABGEBROCHEN§r§c!";

	public static String MESSAGE_INFO =
	        "§7>"
	    + "\n§7> §cScheduled maintenance in: §6%time%"
	    + "\n§7> §cExpected time remaining: §6%remaining%"
	    + "\n§7>";

	public static String MESSAGE_SCHEDULED =
	        "§7>"
	    + "\n§7> §e\u21af§r §6§lMaintenance active§r §e\u21af§r"
	    + "\n§7>"
	    + "\n§7> §cScheduled maintenance in: §6%time%"
	    + "\n§7> §cExpected duration: §6%duration%"
	    + "\n§7>";

	public static String MESSAGE_SHUTDOWN =
	    "§7> §cShutdown in §6%time%§c!";

	public static String MESSAGE_KICK =
	    "§e\u21af§r §6§lMaintenance active§r §e\u21af§r"
	    + "\n§cExpected duration: §6%duration%";

	public static String MOTD =
	    "§e\u21af§r §6§lMaintenance active§r §e\u21af§r"
	    + "\n§cExpected time remaining: §6%remaining%";

	public static String MESSAGE_CONNECT =
	    "%MOTD%"
	    + "\n"
	    + "\n§7Please try again later.";

	private final Waterfall plugin;
	private final File file = new File("./.maintenance");

	private boolean enabled = false;
	private TaskEnable task_enable = new TaskEnable();
	private TaskNotify task_notify = new TaskNotify();

	private long start = 0;
	private long duration = 0;

	public Maintenance(final Waterfall plugin) {
		this.plugin = plugin;
	}

	public long start() { return start; }
	public long duration() { return duration; }
	public boolean enabled() { return enabled; }

	public void enable() {
		enabled = true;

		// Kick all players
		final var kick_message = format_message(MESSAGE_KICK);
		for (final var player : plugin.getProxy().getPlayers()) {
			player.disconnect(kick_message);
		}

		plugin.getLogger().info("Maintenance enabled!");
	}

	public void disable() {
		start = 0;
		duration = 0;
		enabled = false;

		task_enable.cancel();
		task_notify.cancel();

		// Delete file
		file.delete();
	}

	public void abort() {
		if (start == 0) {
			return;
		}

		if (start - System.currentTimeMillis() > 0) {
			// Broadcast message (only if not started yet)
			plugin.getProxy().broadcast(TextComponent.fromLegacyText(MESSAGE_ABORTED));
		}

		// Disable maintenance (just to be on the safe side)
		disable();

		plugin.getLogger().info("Maintenance disabled!");
	}

	public void schedule(long start_millis, long duration_millis) {
		// Schedule maintenance
		enabled = false;
		start = start_millis;
		duration = duration_millis;

		// Save to file
		save();

		// Start tasks
		task_enable.schedule();
		task_notify.schedule();
	}

	public void load() {
		if (file.exists()) {
			// Recover maintenance times

			try (
			    final var file_reader = new FileReader(file);
			    final var reader = new BufferedReader(file_reader)) {
				start = Long.parseLong(reader.readLine());
				duration = Long.parseLong(reader.readLine());
			} catch (IOException | NumberFormatException e) {
				disable();
				return;
			}

			final var delta = System.currentTimeMillis() - start;
			if (delta < 0) {
				// Maintenance scheduled but not active
				schedule(start, duration);
			} else if (delta - duration < 0) {
				// Maintenance still active
				enable();
			} else {
				// Maintenance already over
				disable();
			}
		} else {
			disable();
		}
	}

	public void save() {
		//create and write file
		try (final FileWriter writer = new FileWriter(file)) {
			writer.write(Long.toString(start) + "\n" + Long.toString(duration));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public BaseComponent[] format_message(final String message) {
		var timespan = start - System.currentTimeMillis();
		final String time;

		if (timespan <= 0) {
			time = "Now";
		} else {
			if (timespan % 1000 >= 500) {
				timespan += 1000;
			}
			time = format_time(timespan);
		}

		var remaining = duration + (start - System.currentTimeMillis());
		if (remaining > duration) {
			remaining = duration;
		} else if (remaining < 0) {
			remaining = 0;
		}

		return TextComponent.fromLegacyText(message
		    .replace("%MOTD%", MOTD)
		    .replace("%time%", time)
		    .replace("%duration%", format_time(duration))
		    .replace("%remaining%", format_time(remaining)));
	}

	public class TaskNotify implements Runnable {
		private ScheduledTask task = null;
		private long notify_time = -1;

		@Override
		public synchronized void run() {
			// Broadcast message
			plugin.getProxy().broadcast(format_message(
			    notify_time <= SHUTDOWN_THRESHOLD
			        ? MESSAGE_SHUTDOWN
			        : MESSAGE_SCHEDULED));

			// Schedule next time
			schedule();
		}

		public synchronized void cancel() {
			if (task != null) {
				task.cancel();
				task = null;

				notify_time = -1;
			}
		}

		@SuppressWarnings("deprecation")
		public synchronized void schedule() {
			//cancel if running
			cancel();

			//substract 500 millis so we will never "forget" one step
			final var timespan = start - System.currentTimeMillis() - 500;

			if (notify_time < 0) {
				// First schedule
				plugin.getProxy().broadcast(format_message(MESSAGE_SCHEDULED));
				notify_time = timespan;
			}

			if ((notify_time = next_notify_time()) < 0) {
				// No next time
				return;
			}

			// Schedule for next time
			task = plugin.getProxy().getScheduler().schedule(plugin, this, timespan - notify_time, TimeUnit.MILLISECONDS);
		}

		public long next_notify_time() {
			if (notify_time < 0) {
				return -1;
			}

			for (final var t : NOTIFY_TIMES) {
				if (notify_time - t > 0) {
					return t;
				}
			}

			return -1;
		}
	}

	public class TaskEnable implements Runnable {
		private ScheduledTask task = null;

		@Override
		public synchronized void run() {
			Maintenance.this.enable();
			task = null;
		}

		synchronized void cancel() {
			if (task != null) {
				task.cancel();
				task = null;
			}
		}

		synchronized void schedule() {
			// Cancel if running
			cancel();

			// New task
			var timespan = Maintenance.this.start() - System.currentTimeMillis();
			if (timespan < 0) {
				timespan = 0;
			}

			task = plugin.getProxy().getScheduler().schedule(plugin, this, timespan, TimeUnit.MILLISECONDS);
		}
	}
}
