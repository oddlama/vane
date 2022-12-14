package org.oddlama.velocity.compat.event;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;

import java.util.logging.Level;

public class VelocityCompatPreLoginEvent extends PreLoginEvent {

	final com.velocitypowered.api.event.connection.PreLoginEvent event;

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
		plugin.get_logger().log(Level.WARNING,
				"Denying multiplexer connection from "
						+ event.getConnection().getRemoteAddress()
						+ ": "
						+ (reason.isEmpty() ? "No reason provided" : reason));

		event.setResult(com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.denied(
				LegacyComponentSerializer.legacySection().deserialize(reason.isEmpty() ? "Failed to authorize multiplexer connection" : reason)
		));
	}

	@Override
	public ProxyPendingConnection get_connection() {
		return new VelocityCompatPendingConnection(event.getConnection(), event.getUsername());
	}

	@Override
	public boolean implementation_specific_auth(MultiplexedPlayer multiplexed_player) {
		// Not applicable, all handled in `ProxyGameProfileRequestListener`
		return true;
	}

}

