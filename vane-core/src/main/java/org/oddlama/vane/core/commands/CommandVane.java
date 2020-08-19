package org.oddlama.vane.core.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.oddlama.vane.core.Module;

import org.oddlama.vane.core.command.Command;

import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.Description;

@Name("vane")
@Description("lol")
//@Permission("vane.core.commands.vane") // TODO needed?
//@Usage("§")
public class CommandVane extends Command {
	public CommandVane(Module module) {
		super(module);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_usage);

		// Add reload param
		var reload = params().fixed("reload").ignore_case();
		reload.exec(this::reload_all);
		reload.choose_module().exec(this::reload_module);
	}

	private boolean reload_module(CommandSender sender, Module module) {
		if (module.reload_configuration()) {
			sender.sendMessage("§bvane-" + module.get_name() + ": §areloaded");
		} else {
			sender.sendMessage("§cerror:§6 §bvane-" + module.get_name() + "§6: Invalid configuration");
		}
		return true;
	}

	private boolean reload_all(CommandSender sender) {
		boolean ret = true;
		for (var m : module.core.get_modules()) {
			ret &= reload_module(sender, m);
		}
		return ret;
	}
}
