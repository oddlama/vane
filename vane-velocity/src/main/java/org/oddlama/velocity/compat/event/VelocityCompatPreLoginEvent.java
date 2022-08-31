package org.oddlama.velocity.compat.event;

import net.kyori.adventure.text.Component;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;

public class VelocityCompatPreLoginEvent extends PreLoginEvent {

	com.velocitypowered.api.event.connection.PreLoginEvent event;

	public VelocityCompatPreLoginEvent(VaneProxyPlugin plugin, com.velocitypowered.api.event.connection.PreLoginEvent event) {
		super(plugin);
		this.event = event;
	}

	@Override
	public void cancel() {
		cancel("");
	}

	@Override
	public void cancel(String reason) {
		event.setResult(com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.denied(Component.text(reason)));
	}

	@Override
	public ProxyPendingConnection get_connection() {
		return new VelocityCompatPendingConnection(event.getConnection(), event.getUsername());
	}

	@Override
	public boolean implementation_specific_auth(MultiplexedPlayer multiplexed_player) {
		// TODO
		return false;
	}

}
