package org.oddlama.vane.admin.commands;

import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.oddlama.vane.admin.Admin;
import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.util.Message;

@Name("gamemode")
@Aliases({"gm"})
public class Gamemode extends Command<Admin> {
	@LangMessage
	private Message lang_set;

	public Gamemode(Context<Admin> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().exec_player(this::toggle_gamemode_self);
		params().choose_online_player().exec_player(this::toggle_gamemode_player);
		var gamemode = params().choose_gamemode();
		gamemode.exec_player(this::set_gamemode_self);
		gamemode.choose_online_player().exec(this::set_gamemode);
	}

	private void toggle_gamemode_self(Player player) {
		toggle_gamemode_player(player, player);
	}

	private void toggle_gamemode_player(CommandSender sender, Player player) {
		set_gamemode(sender, player.getGameMode() == GameMode.CREATIVE
		                       ? GameMode.SURVIVAL
		                       : GameMode.CREATIVE, player);
	}

	private void set_gamemode_self(Player player, GameMode mode) {
		set_gamemode(player, mode, player);
	}

	private void set_gamemode(CommandSender sender, GameMode mode, Player player) {
		player.setGameMode(mode);
		sender.sendMessage(lang_set.format(player.getDisplayName(), mode.name()));
	}
}
