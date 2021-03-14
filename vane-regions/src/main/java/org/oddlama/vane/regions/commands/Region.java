package org.oddlama.vane.regions.commands;

import java.util.Collections;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.entity.Player;

import org.oddlama.vane.annotation.command.Aliases;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.regions.Regions;

@Name("region")
@Aliases({"regions", "rg"})
public class Region extends Command<Regions> {
	public Region(Context<Regions> context) {
		super(context);

		// Add help
		params().fixed("help").ignore_case().exec(this::print_help);
		// Command parameters
		params().exec_player(this::open_menu);
	}

	private void open_menu(Player player) {
		get_module().menus.main_menu.create(player).open(player);
	}
}
