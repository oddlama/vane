package org.oddlama.vane.core.commands;

import org.bukkit.command.CommandSender;

import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.Core;

@Name("vane")
public class CommandVane extends Command<Core> {
	public CommandVane(Context<Core> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);

		// Command parameters
		var reload = params().fixed("reload").ignore_case();
		reload.exec(this::reload_all);
		reload.choose_module().exec(this::reload_module);
	}

	private void reload_module(CommandSender sender, Module<?> module) {
		if (module.reload_configuration()) {
			sender.sendMessage("§bvane-" + module.get_name() + ": §areload successful");
		} else {
			sender.sendMessage("§cerror:§6 §bvane-" + module.get_name() + "§6: Invalid configuration");
		}
	}

	private void reload_all(CommandSender sender) {
		for (var m : get_module().core.get_modules()) {
			reload_module(sender, m);
		}
	}
}
