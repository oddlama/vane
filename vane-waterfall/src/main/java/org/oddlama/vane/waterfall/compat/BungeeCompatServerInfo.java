package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.config.ServerInfo;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.waterfall.Waterfall;

import java.net.SocketAddress;

public class BungeeCompatServerInfo implements IVaneProxyServerInfo {

	public ServerInfo serverInfo;

	public BungeeCompatServerInfo(ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}

	@Override
	public String getName() {
		return serverInfo.getName();
	}

	@Override
	public SocketAddress getSocketAddress() {
		return serverInfo.getSocketAddress();
	}

	@Override
	public void sendData(byte[] data) {
		serverInfo.sendData(Waterfall.CHANNEL_AUTH_MULTIPLEX, data);
	}

	@Override
	public boolean sendData(byte[] data, boolean queue) {
		return serverInfo.sendData(Waterfall.CHANNEL_AUTH_MULTIPLEX, data, queue);
	}

}
