package org.oddlama.velocity.compat.event;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.ProxyServer;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.UUID;

public class VelocityCompatPendingConnection implements ProxyPendingConnection {

	final InboundConnection connection;
	final String username;
	final UUID uuid;

	public VelocityCompatPendingConnection(InboundConnection connection, String username) {
		this.connection = connection;
		this.username = username;
		this.uuid = null;
	}

	public VelocityCompatPendingConnection(Player player) {
		this.connection = player;
		this.username = player.getUsername();
		this.uuid = player.getUniqueId();
	}

	@Override
	public String get_name() {
		return username;
	}

	@Override
	public @Nullable UUID get_unique_id() {
		return uuid;
	}

	@Override
	public int get_port() {
		return connection.getVirtualHost().get().getPort();
	}

	@Override
	public SocketAddress get_socket_address() {
		return connection.getVirtualHost().get();
	}

	@Override
	public boolean has_permission(ProxyServer server, String... permission) {
		// Safe cast, we don't use this outside the case where this is constructed
		// with a Player
		return Arrays.stream(permission).anyMatch(perm -> ((Player)connection).hasPermission(perm));
	}

}
