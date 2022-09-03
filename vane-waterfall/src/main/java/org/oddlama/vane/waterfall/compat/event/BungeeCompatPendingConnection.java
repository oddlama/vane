package org.oddlama.vane.waterfall.compat.event;

import net.md_5.bungee.api.connection.PendingConnection;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.ProxyServer;

import java.net.SocketAddress;
import java.util.UUID;

public class BungeeCompatPendingConnection implements ProxyPendingConnection {

	PendingConnection connection;

	public BungeeCompatPendingConnection(PendingConnection connection) {
		this.connection = connection;
	}

	@Override
	public String get_name() {
		return connection.getName();
	}

	@Override
	public @Nullable UUID get_unique_id() {
		return connection.getUniqueId();
	}

	@Override
	public int get_port() {
		return connection.getVirtualHost().getPort();
	}

	@Override
	public SocketAddress get_socket_address() {
		return connection.getSocketAddress();
	}

	@Override
	public boolean has_permission(ProxyServer server, String... permission) {
		return server.has_permission(get_unique_id(), permission);
	}

}
