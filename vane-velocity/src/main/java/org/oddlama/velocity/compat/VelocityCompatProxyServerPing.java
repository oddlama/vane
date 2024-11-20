package org.oddlama.velocity.compat;

import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.util.Favicon;
import net.kyori.adventure.text.Component;
import org.oddlama.vane.proxycore.listeners.ProxyServerPing;

public class VelocityCompatProxyServerPing implements ProxyServerPing {

    public final ServerPing.Builder ping;

    public VelocityCompatProxyServerPing(ServerPing ping) {
        this.ping = ping.asBuilder();
    }

    @Override
    public void set_description(String description) {
        ping.description(Component.text(description));
    }

    @Override
    public void set_favicon(String encoded_favicon) {
        if (encoded_favicon != null) ping.favicon(new Favicon(encoded_favicon));
    }
}
