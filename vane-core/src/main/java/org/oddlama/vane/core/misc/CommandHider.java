package org.oddlama.vane.core.misc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class CommandHider extends Listener<Core> {

	public CommandHider(Context<Core> context) {
		super(
			context.group(
				"hide_commands",
				"Hide error messages for all commands for which a player has no permission, by displaying the default unknown command message instead."
			)
		);
	}

	private boolean allow_command_event(String message, Player player) {
		message = message.trim();
		if (!message.startsWith("/")) {
			return false;
		}

		var id = message.substring(1);
		final var space_index = id.indexOf(' ');
		if (space_index > -1) {
			id = id.substring(0, space_index);
		}

		final var command_map = get_module().getServer().getCommandMap().getKnownCommands();
		var command = command_map.get(id);
		if (command != null) {
			return command.testPermissionSilent(player);
		}

		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_command_preprocess(PlayerCommandPreprocessEvent event) {
		if (!allow_command_event(event.getMessage(), event.getPlayer())) {
			final var msg = get_module().getServer().spigot().getSpigotConfig().getString("messages.unknown-command");
			event.getPlayer().sendMessage(msg);
			event.setCancelled(true);
		}
	}
}
