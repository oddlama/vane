package org.oddlama.vane.waterfall.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.oddlama.vane.waterfall.Waterfall;

public class Ping extends Command {

	private final Waterfall plugin;

	public Ping(final Waterfall plugin) {
		super("ping", "vane_waterfall.commands.ping", new String[0]);
		this.plugin = plugin;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (!(sender instanceof ProxiedPlayer)) {
			sender.sendMessage(TextComponent.fromLegacyText("Not a player!"));
			return;
		}

		if (!hasPermission(sender)) {
			sender.sendMessage(TextComponent.fromLegacyText("No permission!"));
			return;
		}

		final var player = (ProxiedPlayer) sender;
		player.sendMessage(TextComponent.fromLegacyText("§7ping: §3" + Integer.toString(player.getPing()) + "ms"));
	}
}
