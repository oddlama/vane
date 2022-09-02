package org.oddlama.vane.waterfall.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.oddlama.vane.proxycore.commands.ProxyPingCommand;
import org.oddlama.vane.waterfall.Waterfall;
import org.oddlama.vane.waterfall.compat.BungeeCompatProxyCommandSender;
import org.oddlama.vane.waterfall.compat.BungeeCompatProxyPlayer;

public class Ping extends Command {

	ProxyPingCommand cmd;

	public Ping(final Waterfall plugin) {
		super("ping");
		this.cmd = new ProxyPingCommand("vane_proxy.commands.ping", plugin);
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		cmd.execute(sender instanceof final ProxiedPlayer player ?
				new BungeeCompatProxyPlayer(player) :
				new BungeeCompatProxyCommandSender(sender), args);
	}

}
