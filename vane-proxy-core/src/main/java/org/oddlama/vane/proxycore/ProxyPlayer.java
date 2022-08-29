package org.oddlama.vane.proxycore;

import org.oddlama.vane.proxycore.commands.ProxyCommandSender;

import java.util.UUID;

public abstract class ProxyPlayer implements ProxyCommandSender {

	public abstract void disconnect(String message);

	public abstract UUID get_unique_id();

	public abstract int get_ping();

}
