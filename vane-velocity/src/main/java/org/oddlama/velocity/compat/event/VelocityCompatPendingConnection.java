package org.oddlama.velocity.compat.event;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.proxycore.ProxyPendingConnection;

import java.net.SocketAddress;
import java.util.UUID;

public class VelocityCompatPendingConnection implements ProxyPendingConnection {

	InboundConnection connection;
	String username;

	public VelocityCompatPendingConnection(InboundConnection connection, String username) {
		this.connection = connection;
		this.username = username;
	}

	public VelocityCompatPendingConnection(Player player) {
		this.connection = player;
		this.username = player.getUsername();
	}

	@Override
	public String get_name() {
		return username;
	}

	@Override
	public @Nullable UUID get_unique_id() {
		return null;
	}

	@Override
	public int get_port() {
		return connection.getVirtualHost().get().getPort();
	}

	@Override
	public SocketAddress get_socket_address() {
		return connection.getVirtualHost().get();
	}

}
