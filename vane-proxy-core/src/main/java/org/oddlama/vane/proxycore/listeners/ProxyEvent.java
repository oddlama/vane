package org.oddlama.vane.proxycore.listeners;

import org.oddlama.vane.proxycore.ProxyPendingConnection;

public interface ProxyEvent {
    ProxyPendingConnection get_connection();

    void fire();
}
