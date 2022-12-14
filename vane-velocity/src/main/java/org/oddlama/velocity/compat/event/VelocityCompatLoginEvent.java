package org.oddlama.velocity.compat.event;

import com.velocitypowered.api.event.ResultedEvent;
import net.kyori.adventure.text.Component;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.listeners.LoginEvent;

public class VelocityCompatLoginEvent extends LoginEvent {

	final com.velocitypowered.api.event.connection.LoginEvent event;

	public VelocityCompatLoginEvent(com.velocitypowered.api.event.connection.LoginEvent event, VaneProxyPlugin plugin, IVaneProxyServerInfo server_info, ProxyPendingConnection connection) {
		super(plugin, server_info, connection);
		this.event = event;
	}

	@Override
	public void cancel() {
		cancel("");
	}

	@Override
	public void cancel(String reason) {
		event.setResult(ResultedEvent.ComponentResult.denied(Component.text(reason)));
	}

	@Override
	public ProxyPendingConnection get_connection() {
		return null;
	}

}
