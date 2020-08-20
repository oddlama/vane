package org.oddlama.vane.admin.commands;

import org.bukkit.entity.Player;

import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.command.Command;

@Name("spawn")
public class CommandSpawn extends Command<Admin> {
	public CommandSpawn(Context<Admin> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().exec_player(this::tp_spawn);
	}

	private void tp_spawn(Player player) {
		// TODO save world in setspawn in key-value store.
	}
}
