package org.oddlama.vane.proxycore.commands;

import org.oddlama.vane.proxycore.ProxyPlayer;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

public class ProxyPingCommand extends ProxyCommand {

	public ProxyPingCommand(String permission, VaneProxyPlugin plugin) {
		super(permission, plugin);
	}

	@Override
	public void execute(ProxyCommandSender sender, String[] args) {
		if (!(sender instanceof final ProxyPlayer player)) {
			sender.send_message("Not a player!");
			return;
		}

		if (!has_permission(player.get_unique_id())) {
			sender.send_message("No permission!");
			return;
		}

		player.send_message("§7ping: §3" + player.get_ping() + "ms");
	}

}
