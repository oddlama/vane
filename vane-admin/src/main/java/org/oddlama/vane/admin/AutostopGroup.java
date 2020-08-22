package org.oddlama.vane.admin;

import static org.oddlama.vane.util.Util.ms_to_ticks;
import static org.oddlama.vane.util.Util.format_time;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.command.CommandSender;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialSet;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.bukkit.scheduler.BukkitTask;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleGroup;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.annotation.lang.LangString;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.util.Message;

public class AutostopGroup extends ModuleGroup<Admin> {
	@ConfigLong(def = 20 * 60 * 1000, min = 0, desc = "Delay in milliseconds after which to stop the server.")
	public long config_delay;

	@LangString
	public String lang_aborted;
	@LangMessage
	public Message lang_scheduled;
	@LangMessage
	public Message lang_status;
	@LangString
	public String lang_status_not_scheduled;
	@LangString
	public String lang_shutdown;

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
		return start_time + config_delay - System.currentTimeMillis();
	}

	public void abort() { abort(null); }
	public void abort(CommandSender sender) {
		if (task == null) {
			send_message(sender, lang_status_not_scheduled);
			return;
		}

		task.cancel();
		task = null;
		start_time = -1;

		send_message(sender, lang_aborted);
	}

	public void schedule() { schedule(null); }
	public void schedule(CommandSender sender) { schedule(sender, config_delay); }
	public void schedule(CommandSender sender, long delay) {
		if (task != null) {
			abort(sender);
		}

		start_time = System.currentTimeMillis();
		task = schedule_task(() -> {
			send_message(null, lang_shutdown);
			get_module().getServer().shutdown();
		}, ms_to_ticks(delay));

		send_message(sender, lang_scheduled.format(format_time(delay)));
	}

	public void status(CommandSender sender) {
		send_message(sender, lang_status.format(format_time(remaining())));
	}

	private void send_message(CommandSender sender, String message) {
		if (sender != null && sender != get_module().getServer().getConsoleSender()) {
			sender.sendMessage(message);
		}
		get_module().log.info(message);
	}

	@Override
	public void on_enable() {
		if (get_module().getServer().getOnlinePlayers().isEmpty()) {
			schedule();
		}
	}
}
