package org.oddlama.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import org.oddlama.vane.proxycore.commands.ProxyMaintenanceCommand;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatProxyCommandSender;

public class Maintenance implements SimpleCommand {

	ProxyMaintenanceCommand cmd;

	public Maintenance(final Velocity plugin) {
		this.cmd = new ProxyMaintenanceCommand("vane_proxy.commands.ping", plugin);
	}

	@Override
	public void execute(Invocation invocation) {
		cmd.execute(new VelocityCompatProxyCommandSender(invocation.source()), invocation.arguments());
	}

}
