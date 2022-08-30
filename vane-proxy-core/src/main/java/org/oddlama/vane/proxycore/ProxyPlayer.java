package org.oddlama.vane.proxycore;

import org.oddlama.vane.proxycore.commands.ProxyCommandSender;

import java.util.UUID;

public interface ProxyPlayer extends ProxyCommandSender {

	void disconnect(String message);

	UUID get_unique_id();

	long get_ping();

}
