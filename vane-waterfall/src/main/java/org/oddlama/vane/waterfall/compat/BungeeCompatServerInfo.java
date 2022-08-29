package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.config.ServerInfo;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;

import java.net.SocketAddress;

public class BungeeCompatServerInfo implements IVaneProxyServerInfo {

	public ServerInfo serverInfo;

	public BungeeCompatServerInfo(ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}

	@Override
	public String getMotd() {
		return serverInfo.getMotd();
	}

	@Override
	public String getName() {
		return serverInfo.getName();
	}

	@Override
	public String getPermission() {
		return serverInfo.getPermission();
	}

	@Override
	public SocketAddress getSocketAddress() {
		return serverInfo.getSocketAddress();
	}

	@Override
	public boolean isRestricted() {
		return serverInfo.isRestricted();
	}

	@Override
	public void sendData(String channel, byte[] data) {
		serverInfo.sendData(channel, data);
	}

	@Override
	public boolean sendData(String channel, byte[] data, boolean queue) {
		return serverInfo.sendData(channel, data, queue);
	}

}
