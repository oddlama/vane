package org.oddlama.vane.proxycore;

import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.log.IVaneLogger;

import java.util.UUID;

public interface VaneProxyPlugin {

	IVaneLogger logger = null;

	String MESSAGE_MULTIPLEX_MOJANG_AUTH_NO_PERMISSION_KICK =
			"§cYou have no permission to use this auth multiplexer!";

	void register_auth_multiplex_player(
			IVaneProxyServerInfo server,
			int multiplexer_id,
			UUID old_uuid,
			String old_name,
			UUID new_uuid,
			String new_name
	);

	boolean is_online(final IVaneProxyServerInfo server);

	String get_motd(final IVaneProxyServerInfo server);

	java.io.File getVaneDataFolder();

	ProxyServer getVaneProxy();

	@NotNull
	IVaneLogger getVaneLogger();

	@NotNull
	Maintenance get_maintenance();

	void try_start_server(ManagedServer server);

}
