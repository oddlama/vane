package org.oddlama.vane.proxycore;

import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.proxycore.config.ConfigManager;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.log.IVaneLogger;

import java.util.LinkedHashMap;
import java.util.UUID;

public interface VaneProxyPlugin {

	IVaneLogger logger = null;

	boolean is_online(final IVaneProxyServerInfo server);

	String get_motd(final IVaneProxyServerInfo server);

	java.io.File getVaneDataFolder();

	ProxyServer getVaneProxy();

	@NotNull
	IVaneLogger getVaneLogger();

	@NotNull
	Maintenance get_maintenance();

	@NotNull
	ConfigManager get_config();

	void try_start_server(ManagedServer server);

	boolean can_join_maintenance(UUID uuid);

	LinkedHashMap<UUID, UUID> get_multiplexed_uuids();
}
