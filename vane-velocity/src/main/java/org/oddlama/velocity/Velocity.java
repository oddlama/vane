package org.oddlama.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.network.ConnectionManager;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import org.bstats.velocity.Metrics;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.log.slf4jCompatLogger;
import org.oddlama.vane.proxycore.util.Version;
import org.oddlama.velocity.commands.Maintenance;
import org.oddlama.velocity.commands.Ping;
import org.oddlama.velocity.compat.VelocityCompatProxyServer;
import org.oddlama.velocity.listeners.*;
import org.slf4j.Logger;

@Plugin(
    id = "vane-velocity",
    name = "Vane Velocity",
    version = Version.VERSION,
    description = "TODO",
    authors = { "oddlama", "Serial-ATA" },
    url = "https://github.com/oddlama/vane"
)
public class Velocity extends VaneProxyPlugin {

    public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create(
        CHANNEL_AUTH_MULTIPLEX_NAMESPACE,
        CHANNEL_AUTH_MULTIPLEX_NAME
    );
    private final ProxyServer velocity_server;

    // bStats
    @SuppressWarnings("unused")
    private final Metrics.Factory metricsFactory;

    @Inject
    public Velocity(
        ProxyServer server,
        Logger logger,
        Metrics.Factory metricsFactory,
        @DataDirectory final Path data_dir
    ) {
        this.server = new VelocityCompatProxyServer(server);
        this.logger = new slf4jCompatLogger(logger);

        this.metricsFactory = metricsFactory;

        this.velocity_server = server;
        this.data_dir = data_dir.toFile();
    }

    public ProxyServer get_raw_proxy() {
        return velocity_server;
    }

    @Subscribe
    public void on_enable(final ProxyInitializeEvent event) {
        if (!config.load()) {
            disable();
            return;
        }

        metricsFactory.make(this, 8891);

        EventManager event_manager = velocity_server.getEventManager();

        event_manager.register(this, new ProxyPingListener(this));
        event_manager.register(this, new ProxyPreLoginListener(this));
        event_manager.register(this, new ProxyGameProfileRequestListener(this));
        event_manager.register(this, new ProxyLoginListener(this));
        event_manager.register(this, new ProxyDisconnectListener(this));

        maintenance.load();

        CommandManager command_manager = velocity_server.getCommandManager();

        CommandMeta ping_meta = command_manager.metaBuilder("ping").build();
        command_manager.register(ping_meta, new Ping(this));

        CommandMeta maintenance_meta = command_manager.metaBuilder("maintenance").build();
        command_manager.register(maintenance_meta, new Maintenance(this));

        velocity_server.getChannelRegistrar().register(CHANNEL);

        if (!config.multiplexer_by_id.isEmpty()) {
            try {
                get_logger().log(Level.INFO, "Attempting to register auth multiplexers");

                // Velocity doesn't let you register multiple listeners like Bungeecord,
                // So we have to take matters into our own hands :)
                handle_listeners("Registering", ConnectionManager::bind);
            } catch (Exception e) {
                get_logger().log(Level.SEVERE, "Failed to inject into VelocityServer!", e);
                disable();
            }
        }
    }

    @Subscribe
    public void on_disable(final ProxyShutdownEvent event) {
        disable();
    }

    private void disable() {
        velocity_server.getEventManager().unregisterListeners(this);

        velocity_server.getChannelRegistrar().unregister(CHANNEL);

        // Now let's be good and clean up our mess :)
        try {
            get_logger().log(Level.INFO, "Attempting to close auth multiplexers");

            handle_listeners("Closing", ConnectionManager::close);
        } catch (Exception e) {
            get_logger().log(Level.SEVERE, "Failed to stop listeners!", e);
            get_logger().log(Level.SEVERE, "Shutting down the server to prevent lingering unmanaged listeners!");
            velocity_server.shutdown();
        }

        server = null;
        logger = null;
    }

    private void handle_listeners(
        String action,
        BiConsumer<? super ConnectionManager, ? super InetSocketAddress> method
    ) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        final var server = (VelocityServer) velocity_server;

        // We steal the VelocityServer's `ConnectionManager`, which (currently) has no
        // issue binding to however many addresses we give it.
        final var velocity_server = Class.forName("com.velocitypowered.proxy.VelocityServer");
        final var cm_field = velocity_server.getDeclaredField("cm");
        cm_field.setAccessible(true);

        final var cm = (ConnectionManager) cm_field.get(server);

        for (final var multiplexer_map : config.multiplexer_by_id.entrySet()) {
            final var id = multiplexer_map.getKey();
            final var port = multiplexer_map.getValue().port;

            get_logger().log(Level.INFO, action + " multiplexer ID " + id + ", bound to port " + port);

            final var address = new InetSocketAddress(port);
            method.accept(cm, address);
        }
    }
}
