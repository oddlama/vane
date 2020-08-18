package org.oddlama.vane.core.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.oddlama.vane.core.Module;

import org.oddlama.vane.core.Command;

import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.Description;

@Name("vane")
@Description("lol")
//@Permission("vane.core.commands.vane") // TODO needed?
//@Description("Yesss")
//@Usage("§")
public class CommandVane extends Command {
	public CommandVane(Module module) {
		super(module);
	}
}
