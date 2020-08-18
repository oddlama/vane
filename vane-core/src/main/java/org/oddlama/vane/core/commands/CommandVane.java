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

		var reload = fixed_param("reload");
		reload.exec(this::reload_all);
		reload.choice_param(module.core.get_modules(), m -> m.get_name())
			.exec(this::reload_module);
	}

	private boolean reload_module(CommandSender sender, Module module) {
		if (module.reload_configuration()) {
			sender.sendMessage("§cerror:§r could not reload §3vane-" + module.get_name() + "§r: Invalid configuration");
		} else {
			sender.sendMessage("§areloaded§r §3vane-" + module.get_name() + "§r");
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
