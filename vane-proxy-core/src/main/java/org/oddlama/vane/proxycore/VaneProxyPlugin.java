package org.oddlama.vane.proxycore;

import org.jetbrains.annotations.NotNull;
import org.oddlama.vane.proxycore.config.ConfigManager;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.config.ManagedServer;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.vane.proxycore.log.IVaneLogger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.UUID;

public abstract class VaneProxyPlugin {

	// TODO: rename
	public static final String CHANNEL_AUTH_MULTIPLEX_NAMESPACE = "vane_waterfall";
	public static final String CHANNEL_AUTH_MULTIPLEX_NAME = "auth_multiplex";
	public static final String CHANNEL_AUTH_MULTIPLEX = CHANNEL_AUTH_MULTIPLEX_NAMESPACE + ":" + CHANNEL_AUTH_MULTIPLEX_NAME;

	private final LinkedHashMap<UUID, UUID> multiplexedUUIDs = new LinkedHashMap<>();
	private final LinkedHashMap<UUID, PreLoginEvent.MultiplexedPlayer> pending_multiplexer_logins = new LinkedHashMap<>();
	public ConfigManager config = new ConfigManager(this);
	public Maintenance maintenance = new Maintenance(this);
	public IVaneLogger logger;
	public ProxyServer server;
	public File data_dir;

	public boolean is_online(final IVaneProxyServerInfo server) {
		final var addr = server.getSocketAddress();
		if (!(addr instanceof final InetSocketAddress inet_addr)) {
			return false;
		}

		var connected = false;
		try (final var test = new Socket(inet_addr.getHostName(), inet_addr.getPort())) {
			connected = test.isConnected();
		} catch (IOException e) {
			// Server not up or not reachable
		}

		return connected;
	}

	public String get_motd(final IVaneProxyServerInfo server) {
		// Maintenance
		if (maintenance.enabled()) {
			return maintenance.format_message(Maintenance.MOTD);
		}

		final var cms = config.managed_servers.get(server.getName());
		if (cms == null) {
			return "";
		}

		String motd;
		if (is_online(server)) {
			motd = cms.motd(ManagedServer.MotdSource.ONLINE);
		} else {
			motd = cms.motd(ManagedServer.MotdSource.OFFLINE);
		}

		return motd;
	}

	public File get_data_folder() {
		return data_dir;
	}

	public ProxyServer get_proxy() {
		return server;
	}

	public @NotNull IVaneLogger get_logger() {
		return logger;
	}

	public @NotNull Maintenance get_maintenance() {
		return this.maintenance;
	}

	public @NotNull ConfigManager get_config() {
		return this.config;
	}

	public void try_start_server(ManagedServer server) {
		// TODO: This could really use some checks (existing process running, nonzero exit code)
		this.server.get_scheduler()
				.runAsync(
						this,
						() -> {
							try {
								final var p = Runtime.getRuntime().exec(server.start_cmd());
								p.waitFor();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
				);
	}

	public boolean can_join_maintenance(UUID uuid) {
		if (maintenance.enabled()) {
			// Client is connecting while maintenance is on
			// Players with bypass_maintenance flag may join
			return this.server.has_permission(uuid, "vane_waterfall.bypass_maintenance");
		}

		return true;
	}

	public LinkedHashMap<UUID, UUID> get_multiplexed_uuids() {
		return multiplexedUUIDs;
	}

	public LinkedHashMap<UUID, PreLoginEvent.MultiplexedPlayer> get_pending_multiplexer_logins() {
		return pending_multiplexer_logins;
	}

}
