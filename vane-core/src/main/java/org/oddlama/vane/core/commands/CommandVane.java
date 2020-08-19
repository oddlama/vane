package org.oddlama.vane.core.commands;

import org.bukkit.command.CommandSender;

import org.oddlama.vane.annotation.command.Description;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.Usage;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.command.Command;

// TODO localized strings....
@Name("vane")
@Usage("§areload §8[§bmodule§8]")
@Description("§areload§7:§r Reloads the given vane module, or all modules if no module is specified.")
public class CommandVane extends Command {
	public CommandVane(Module module) {
		super(module);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);

		// Command parameters
		var reload = params().fixed("reload").ignore_case();
		reload.exec(this::reload_all);
		reload.choose_module().exec(this::reload_module);
	}

	private void reload_module(CommandSender sender, Module module) {
		if (module.reload_configuration()) {
			sender.sendMessage("§bvane-" + module.get_name() + ": §areload successful");
		} else {
			sender.sendMessage("§cerror:§6 §bvane-" + module.get_name() + "§6: Invalid configuration");
		}
	}

	private void reload_all(CommandSender sender) {
		for (var m : module.core.get_modules()) {
			reload_module(sender, m);
		}
	}
}
