package org.oddlama.velocity.compat.event;

import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import org.oddlama.vane.proxycore.ProxyPendingConnection;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.listeners.PingEvent;
import org.oddlama.velocity.compat.VelocityCompatProxyServerPing;

public class VelocityCompatPingEvent extends PingEvent {

    final ProxyPingEvent event;

    public VelocityCompatPingEvent(VaneProxyPlugin plugin, ProxyPingEvent event, IVaneProxyServerInfo server) {
        super(plugin, new VelocityCompatProxyServerPing(event.getPing()), server);
        this.event = event;
    }

    @Override
    public void send_response() {
        event.setPing(((VelocityCompatProxyServerPing) ping).ping.build());
    }

    @Override
    public ProxyPendingConnection get_connection() {
        return null;
    }
}
