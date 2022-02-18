package org.oddlama.vane.admin.commands;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;

@Name("spawn")
public class Spawn extends Command<Admin> {

	public Spawn(Context<Admin> context) {
		super(context);
		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().exec_player(this::tp_spawn);
	}

	private void tp_spawn(Player player) {
		if (get_module().storage_spawn_location != null && get_module().storage_spawn_location.location().isWorldLoaded()) {
			player.teleport(get_module().storage_spawn_location.location(), TeleportCause.COMMAND);
		}
	}
}
