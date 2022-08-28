package org.oddlama.vane.waterfall.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Ping extends Command {

	public Ping() {
		super("ping", "vane_waterfall.commands.ping");
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (!(sender instanceof final ProxiedPlayer player)) {
			sender.sendMessage(TextComponent.fromLegacyText("Not a player!"));
			return;
		}

		if (!hasPermission(sender)) {
			sender.sendMessage(TextComponent.fromLegacyText("No permission!"));
			return;
		}

		player.sendMessage(TextComponent.fromLegacyText("§7ping: §3" + player.getPing() + "ms"));
	}

}
