package org.oddlama.vane.proxycore.commands;

import org.oddlama.vane.proxycore.VaneProxyPlugin;

import java.util.UUID;

public abstract class ProxyCommand {

	public final String permission;
	public final VaneProxyPlugin plugin;

	public ProxyCommand(String permission, VaneProxyPlugin plugin) {
		this.permission = permission;
		this.plugin = plugin;
	}

	public abstract void execute(ProxyCommandSender sender, String[] args);

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean has_permission(UUID uuid) {
		return this.permission == null || plugin.get_proxy().has_permission(uuid, this.permission);
	}

}
