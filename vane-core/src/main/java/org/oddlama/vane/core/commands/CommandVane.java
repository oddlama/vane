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
//@AllowedSenders({ Player.class, })
public class CommandVane extends Command {
	public CommandVane(Module module) {
		super(module);

		var reload = params().fixed("reload");
		reload.exec(this::reload_all);
			// TODO ignore case on some comparisons
		reload.choose_module()
			// TODO test no exec
			.exec(this::reload_module);
		params().any_string()
			.choose_module().exec(this::reload_module);
	}

	private boolean reload_module(CommandSender sender, Module module) {
		if (module.reload_configuration()) {
			sender.sendMessage("§cerror:§6 could not reload §bvane-" + module.get_name() + "§6: Invalid configuration");
		} else {
			sender.sendMessage("§areloaded§r §bvane-" + module.get_name());
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
