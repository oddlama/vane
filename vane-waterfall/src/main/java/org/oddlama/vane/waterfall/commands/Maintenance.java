package org.oddlama.vane.waterfall.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import org.oddlama.vane.proxycore.commands.ProxyMaintenanceCommand;
import org.oddlama.vane.waterfall.Waterfall;
import org.oddlama.vane.waterfall.compat.BungeeCompatProxyCommandSender;

public class Maintenance extends Command {

	ProxyMaintenanceCommand cmd;

	public Maintenance(final Waterfall plugin) {
		super("maintenance");
		this.cmd = new ProxyMaintenanceCommand("vane_proxy.commands.maintenance", plugin);
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		cmd.execute(new BungeeCompatProxyCommandSender(sender), args);
	}

}
