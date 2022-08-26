package org.oddlama.vane.admin;

import static org.oddlama.vane.util.TimeUtil.format_time;
import static org.oddlama.vane.util.Conversions.ms_to_ticks;

import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleGroup;

public class AutostopGroup extends ModuleGroup<Admin> {

	@ConfigLong(def = 20 * 60, min = 0, desc = "Delay in seconds after which to stop the server.")
	public long config_delay;

	@LangMessage
	public TranslatedMessage lang_aborted;

	@LangMessage
	public TranslatedMessage lang_scheduled;

	@LangMessage
	public TranslatedMessage lang_status;

	@LangMessage
	public TranslatedMessage lang_status_not_scheduled;

	@LangMessage
	public TranslatedMessage lang_shutdown;

	// Variables
	public BukkitTask task = null;
	public long start_time = -1;

	public AutostopGroup(Context<Admin> context) {
		super(context, "autostop", "Enable automatic server stop after certain time without online players.");
	}

	public long remaining() {
		if (start_time == -1) {
			return -1;
		}
		return start_time + (config_delay * 1000) - System.currentTimeMillis();
	}

	public void abort() {
		abort(null);
	}

	public void abort(CommandSender sender) {
		if (task == null) {
			lang_status_not_scheduled.send(sender);
			return;
		}

		task.cancel();
		task = null;
		start_time = -1;

		lang_aborted.send_and_log(sender);
	}

	public void schedule() {
		schedule(null);
	}

	public void schedule(CommandSender sender) {
		schedule(sender, config_delay * 1000);
	}

	public void schedule(CommandSender sender, long delay) {
		if (task != null) {
			abort(sender);
		}

		start_time = System.currentTimeMillis();
		task =
			schedule_task(
				() -> {
					lang_shutdown.send_and_log(null);
					get_module().getServer().shutdown();
				},
				ms_to_ticks(delay)
			);

		lang_scheduled.send_and_log(sender, "§b" + format_time(delay));
	}

	public void status(CommandSender sender) {
		if (task == null) {
			lang_status_not_scheduled.send(sender);
			return;
		}

		lang_status.send(sender, "§b" + format_time(remaining()));
	}

	@Override
	public void on_enable() {
		if (get_module().getServer().getOnlinePlayers().isEmpty()) {
			schedule();
		}
	}
}
