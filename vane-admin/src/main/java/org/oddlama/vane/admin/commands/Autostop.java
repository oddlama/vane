package org.oddlama.vane.admin.commands;

import static org.oddlama.vane.util.TimeUtil.parse_time;

import org.bukkit.command.CommandSender;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.admin.AutostopGroup;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;

@Name("autostop")
public class Autostop extends Command<Admin> {

	AutostopGroup autostop;

	public Autostop(AutostopGroup context) {
		super(context);
		this.autostop = context;

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().exec(this::status);
		params().fixed("abort").ignore_case().exec(this::abort);
		params().fixed("status").ignore_case().exec(this::status);
		var schedule = params().fixed("schedule").ignore_case();
		schedule.exec(this::schedule);
		schedule.any_string().exec(this::schedule_delay);
	}

	private void status(CommandSender sender) {
		autostop.status(sender);
	}

	private void abort(CommandSender sender) {
		autostop.abort(sender);
	}

	private void schedule(CommandSender sender) {
		autostop.schedule(sender);
	}

	private void schedule_delay(CommandSender sender, String delay) {
		try {
			autostop.schedule(sender, parse_time(delay));
		} catch (NumberFormatException e) {
			get_module().core.lang_invalid_time_format.send(sender, e.getMessage());
		}
	}
}
