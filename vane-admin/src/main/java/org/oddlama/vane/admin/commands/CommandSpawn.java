package org.oddlama.vane.admin.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.oddlama.vane.annotation.command.Description;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.command.Usage;
import org.oddlama.vane.core.Module;
import org.oddlama.vane.core.command.Command;

@Name("spawn")
@Usage("")
@Description("Teleports you to the spawn.")
public class CommandSpawn extends Command {
	public CommandSpawn(Module module) { // TODO pass admin to get localization?
		super(module);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().exec_player(this::tp_spawn);
	}

	private void tp_spawn(Player player, Module module) {
		// TODO save world in setspawn in key-value store.
	}
}
