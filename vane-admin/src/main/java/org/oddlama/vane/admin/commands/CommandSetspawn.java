package org.oddlama.vane.admin.commands;

import org.bukkit.entity.Player;

import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.command.Command;

@Name("setspawn")
public class CommandSetspawn extends Command<Admin> {
	public CommandSetspawn(Context<Admin> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().exec_player(this::set_spawn);
	}

	private void set_spawn(Player player) {
		player.getWorld().setSpawnLocation(player.getLocation());
		player.sendMessage("§aSpawn §7set!");
	}
}
