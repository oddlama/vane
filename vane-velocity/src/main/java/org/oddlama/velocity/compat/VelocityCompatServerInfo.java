package org.oddlama.velocity.compat;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.net.SocketAddress;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.velocity.Velocity;

public class VelocityCompatServerInfo implements IVaneProxyServerInfo {

    public final RegisteredServer server;

    public VelocityCompatServerInfo(RegisteredServer server) {
        this.server = server;
    }

    @Override
    public String getName() {
        return server.getServerInfo().getName();
    }

    @Override
    public SocketAddress getSocketAddress() {
        return server.getServerInfo().getAddress();
    }

    @Override
    public void sendData(byte[] data) {
        server.sendPluginMessage(Velocity.CHANNEL, data);
    }

    @Override
    public boolean sendData(byte[] data, boolean queue) {
        // Not applicable
        assert false;
        return false;
    }
}
