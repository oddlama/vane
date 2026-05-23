package org.oddlama.vane.proxycore;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.proxycore.config.ConfigManager;
import org.oddlama.vane.proxycore.config.IVaneProxyServerInfo;
import org.oddlama.vane.proxycore.config.ManagedServer;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.vane.proxycore.log.IVaneLogger;

public abstract class VaneProxyPlugin {

    public static final String CHANNEL_AUTH_MULTIPLEX_NAMESPACE = "vane_proxy";
    public static final String CHANNEL_AUTH_MULTIPLEX_NAME = "auth_multiplex";
    public static final String CHANNEL_AUTH_MULTIPLEX =
        CHANNEL_AUTH_MULTIPLEX_NAMESPACE + ":" + CHANNEL_AUTH_MULTIPLEX_NAME;

    public ConfigManager config = new ConfigManager(this);
    public Maintenance maintenance = new Maintenance(this);
    public IVaneLogger logger;
    public ProxyServer server;
    public File data_dir;

    private final LinkedHashMap<UUID, UUID> multiplexedUUIDs = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, PreLoginEvent.MultiplexedPlayer> pending_multiplexer_logins =
        new LinkedHashMap<>();
    private boolean server_starting;

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
        if (cms == null) return "";

        ManagedServer.ConfigItemSource source;
        if (is_online(server)) {
            source = ManagedServer.ConfigItemSource.ONLINE;
        } else {
            source = ManagedServer.ConfigItemSource.OFFLINE;
        }

        return cms.motd(source);
    }

    public @Nullable String get_favicon(final IVaneProxyServerInfo server) {
        final var cms = config.managed_servers.get(server.getName());
        if (cms == null) return null;

        ManagedServer.ConfigItemSource source;
        if (is_online(server)) {
            source = ManagedServer.ConfigItemSource.ONLINE;
        } else {
            source = ManagedServer.ConfigItemSource.OFFLINE;
        }

        return cms.favicon(source);
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
        // FIXME: this is not async-safe and there might be conditions where two start commands can
        // be executed
        // simultaneously. Don't rely on this as a user - instead use a start command that is
        // atomic.
        if (server_starting) return;

        this.server.get_scheduler()
            .runAsync(this, () -> {
                try {
                    server_starting = true;
                    get_logger()
                        .log(
                            Level.INFO,
                            "Running start command for server '" +
                            server.id() +
                            "': " +
                            Arrays.toString(server.start_cmd())
                        );
                    final var timeout = server.command_timeout();

                    final var processBuilder = new ProcessBuilder(server.start_cmd());
                    processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
                    final var process = processBuilder.start();

                    if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
                        get_logger().log(Level.SEVERE, "Server '" + server.id() + "'s start command timed out!");
                    }

                    if (process.exitValue() != 0) {
                        get_logger()
                            .log(
                                Level.SEVERE,
                                "Server '" + server.id() + "'s start command returned a nonzero exit code!"
                            );
                    }
                } catch (Exception e) {
                    get_logger().log(Level.SEVERE, "Failed to start server '" + server.id() + "'", e);
                }

                server_starting = false;
            });
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean can_join_maintenance(UUID uuid) {
        if (maintenance.enabled()) {
            // Client is connecting while maintenance is on
            // Players with a bypass_maintenance flag may join
            return this.server.has_permission(uuid, "vane_proxy.bypass_maintenance");
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
