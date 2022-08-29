package org.oddlama.vane.proxycore.config;

import java.net.SocketAddress;

public interface IVaneProxyServerInfo {

	String getMotd();

	String getName();

	String getPermission();

	SocketAddress getSocketAddress();

	boolean isRestricted();

	void sendData(String channel, byte[] data);

	boolean sendData(String channel, byte[] data, boolean queue);

}
