package org.oddlama.vane.waterfall.commands;
import org.oddlama.vane.waterfall.Waterfall;
import net.md_5.bungee.api.chat.TextComponent;

import static org.oddlama.vane.waterfall.Util.parse_time;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class Maintenance extends Command {
	public static String MESSAGE_INVALID_TIME_FORMAT = "§cInvalid time format §6'%time%'§c!";

	private final Waterfall plugin;

	public Maintenance(final Waterfall plugin) {
		super("maintentance", "vane_waterfall.commands.maintenance", new String[0]);
		this.plugin = plugin;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		// Only check permission on players
		if (sender instanceof ProxiedPlayer && !hasPermission(sender)) {
			sender.sendMessage(TextComponent.fromLegacyText("No permission!"));
			return;
		}

		if (args.length == 1 && (args[0].equalsIgnoreCase("status"))) {
			if (plugin.maintenance.start() != 0) {
				sender.sendMessage(plugin.maintenance.format_message(org.oddlama.vane.waterfall.Maintenance.MESSAGE_INFO));
			} else {
			}
		} else if (args.length == 1 && (args[0].equalsIgnoreCase("cancel"))) {
			plugin.maintenance.abort();
		} else if (args.length == 3 && (args[0].equalsIgnoreCase("schedule"))) {
			long time = 0;
			long duration = 0;

			try {
				time = parse_time(args[1]);
			} catch (NumberFormatException e) {
				sender.sendMessage(TextComponent.fromLegacyText(MESSAGE_INVALID_TIME_FORMAT.replace("%time%", args[1])));
				return;
			}

			try {
				duration = parse_time(args[2]);
			} catch (NumberFormatException e) {
				sender.sendMessage(TextComponent.fromLegacyText(MESSAGE_INVALID_TIME_FORMAT.replace("%time%", args[2])));
				return;
			}

			plugin.maintenance.schedule(System.currentTimeMillis() + time, duration);
		} else {
			sender.sendMessage(TextComponent.fromLegacyText(
				    "§7> §3/maintenance §3[ §7cancel §3] §f- Cancel any scheduled/active maintenance"
				+ "\n§7> §3/maintenance §3[ §7status §3] §f- Display info about scheduled/active maintenance"
				+ "\n§7> §3/maintenance §3[ §7schedule §3] <§bin§7> <§bduration§7> §f- Schedule maintenance in <in> for <duration>"
				+ "\n§7> §3|§7 time format§7 §f- Examples: §b§o3h5m§r§f or §b§o1y2w3d4h5m6s§r"));
		}
	}
}
