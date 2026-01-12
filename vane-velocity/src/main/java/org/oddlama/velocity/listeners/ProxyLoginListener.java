package org.oddlama.velocity.listeners;

import static org.oddlama.velocity.Util.get_server_for_host;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.proxy.ProxyServer;
import org.oddlama.vane.proxycore.listeners.LoginEvent;
import org.oddlama.velocity.Velocity;
import org.oddlama.velocity.compat.VelocityCompatServerInfo;
import org.oddlama.velocity.compat.event.VelocityCompatLoginEvent;
import org.oddlama.velocity.compat.event.VelocityCompatPendingConnection;

public class ProxyLoginListener {

    final Velocity velocity;

    @Inject
    public ProxyLoginListener(Velocity velocity) {
        this.velocity = velocity;
    }

    @Subscribe(priority = 0)
    public void login(com.velocitypowered.api.event.connection.LoginEvent event) {
        if (!event.getResult().isAllowed()) return;

        ProxyServer proxy = velocity.get_raw_proxy();

        final var virtual_host = event.getPlayer().getVirtualHost();
        if (virtual_host.isEmpty()) return;

        final var server = get_server_for_host(proxy, virtual_host.get());

        var server_info = new VelocityCompatServerInfo(server);
        LoginEvent proxy_event = new VelocityCompatLoginEvent(
            event,
            velocity,
            server_info,
            new VelocityCompatPendingConnection(event.getPlayer())
        );
        proxy_event.fire();
    }
}
