package org.oddlama.vane.proxycore;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ProxyPendingConnection {

	String get_name();

	@Nullable
	UUID get_unique_id();

	int get_port();

	java.net.SocketAddress get_socket_address();

}
