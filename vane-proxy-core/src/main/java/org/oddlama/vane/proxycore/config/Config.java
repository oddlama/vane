package org.oddlama.vane.proxycore.config;

import org.oddlama.vane.proxycore.ManagedServer;

import java.util.LinkedHashMap;

public class Config {

	// port, multiplexer_id
	public LinkedHashMap<Integer, Integer> auth_multiplex;
	public LinkedHashMap<String, ManagedServer> managed_servers;

	public Config() {
	}

}
