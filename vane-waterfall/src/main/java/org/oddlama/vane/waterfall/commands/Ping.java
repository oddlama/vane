package org.oddlama.vane.waterfall.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import org.oddlama.vane.proxycore.commands.ProxyPingCommand;
import org.oddlama.vane.waterfall.Waterfall;
import org.oddlama.vane.waterfall.compat.BungeeCompatProxyCommandSender;

public class Ping extends Command {

	ProxyPingCommand cmd;

	public Ping(final Waterfall plugin) {
		super("ping");
		this.cmd = new ProxyPingCommand("vane_waterfall.commands.ping", plugin);
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		cmd.execute(new BungeeCompatProxyCommandSender(sender), args);
	}

}
