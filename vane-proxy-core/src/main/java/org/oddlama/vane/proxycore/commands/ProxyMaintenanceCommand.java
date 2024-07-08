package org.oddlama.vane.proxycore.commands;

import org.oddlama.vane.proxycore.Maintenance;
import org.oddlama.vane.proxycore.ProxyPlayer;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

import static org.oddlama.vane.proxycore.util.TimeUtil.parse_time;

public class ProxyMaintenanceCommand extends ProxyCommand {

	public static String MESSAGE_INVALID_TIME_FORMAT = "§cInvalid time format §6'%time%'§c!";

	public ProxyMaintenanceCommand(String permission, VaneProxyPlugin plugin) {
		super(permission, plugin);
	}

	public void execute(ProxyCommandSender sender, String[] args) {
		// Only check permission on players
		if (sender instanceof ProxyPlayer player && !has_permission(player.get_unique_id())) {
			sender.send_message("No permission!");
			return;
		}

		Maintenance maintenance = plugin.get_maintenance();

		if (args.length == 1) {
			switch (args[0].toLowerCase()) {
				case "status" -> {
					if (maintenance.start() != 0) {
						sender.send_message(
								maintenance.format_message(org.oddlama.vane.proxycore.Maintenance.MESSAGE_INFO));
					}

					return;
				}
				case "on" -> {
					maintenance.schedule(System.currentTimeMillis(), null);
					return;
				}
				case "cancel", "off" -> {
					maintenance.abort();
					return;
				}
			}
		} else if (args.length == 3 && (args[0].equalsIgnoreCase("schedule"))) {
			long time;
			long duration;

			try {
				time = parse_time(args[1]);
			} catch (NumberFormatException e) {
				sender.send_message(
						MESSAGE_INVALID_TIME_FORMAT.replace("%time%", args[1]));
				return;
			}

			try {
				duration = parse_time(args[2]);
			} catch (NumberFormatException e) {
				sender.send_message(
						MESSAGE_INVALID_TIME_FORMAT.replace("%time%", args[2]));
				return;
			}

			maintenance.schedule(System.currentTimeMillis() + time, duration);
			return;
		}

		sender.send_message(
				"""
						§7> §3/maintenance §3[ §7cancel§r|§7off §3] §f- Cancel any scheduled/active maintenance
						§7> §3/maintenance §3[ §7status §3] §f- Display info about scheduled/active maintenance
						§7> §3/maintenance §3[ §7on §3] §f- Enable maintenance for an indefinite amount of time
						§7> §3/maintenance §3[ §7schedule §3] §7<§bin§7> <§bduration§7> §f- Schedule maintenance in <in> for <duration>
						§7> §3|§7 time format§7 §f- Examples: §b§o3h5m§r§f or §b§o1y2w3d4h5m6s§r""");
	}

}
