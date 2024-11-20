package org.oddlama.vane.proxycore;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface ProxyPendingConnection {
    String get_name();

    @Nullable
    UUID get_unique_id();

    int get_port();

    java.net.SocketAddress get_socket_address();

    boolean has_permission(ProxyServer server, final String... permission);

    default boolean can_start_server(ProxyServer server, String serverName) {
        return has_permission(
            server,
            "vane_proxy.start_server",
            "vane_proxy.start_server.*",
            "vane_proxy.start_server." + serverName
        );
    }
}
