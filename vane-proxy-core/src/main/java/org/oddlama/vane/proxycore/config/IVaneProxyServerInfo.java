package org.oddlama.vane.proxycore.config;

import java.net.SocketAddress;

public interface IVaneProxyServerInfo {

	String getName();

	SocketAddress getSocketAddress();

	void sendData(byte[] data);

	boolean sendData(byte[] data, boolean queue);

}
