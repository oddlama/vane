package org.oddlama.vane.admin.commands;

import org.oddlama.vane.admin.AutostopGroup;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.Context;
import org.bukkit.command.CommandSender;
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
		params().fixed("schedule").ignore_case().exec(this::schedule);
		params().fixed("status").ignore_case().exec(this::status);
	}

	private void status(CommandSender sender) { autostop.status(sender); }
	private void abort(CommandSender sender) { autostop.abort(sender); }
	private void schedule(CommandSender sender) { autostop.schedule(sender); }
}
