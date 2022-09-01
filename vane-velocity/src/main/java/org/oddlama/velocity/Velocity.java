package org.oddlama.velocity;

import com.google.inject.Inject;
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
import org.bstats.velocity.Metrics;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.listeners.PreLoginEvent;
import org.oddlama.vane.proxycore.log.slf4jCompatLogger;
import org.oddlama.vane.util.Version;
import org.oddlama.velocity.compat.VelocityCompatProxyServer;
import org.oddlama.velocity.listeners.*;
import org.slf4j.Logger;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.logging.Level;

@Plugin(id = "vane-velocity", name = "Vane Velocity", version = Version.VERSION, description = "TODO",
		authors = {"oddlama", "Serial-ATA"}, url = "https://github.com/oddlama/vane")
public class Velocity extends VaneProxyPlugin {

	public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create(CHANNEL_AUTH_MULTIPLEX_NAMESPACE, CHANNEL_AUTH_MULTIPLEX_NAME);
	private final ProxyServer velocity_server;
	private final File data_dir;
	public final LinkedHashMap<UUID, PreLoginEvent.MultiplexedPlayer> pending_multiplexer_logins = new LinkedHashMap<>();
	// bStats
	@SuppressWarnings("unused")
	private final Metrics.Factory metricsFactory;

	@Inject
	public Velocity(ProxyServer server, Logger logger, Metrics.Factory metricsFactory, @DataDirectory final Path data_dir) {
		this.server = new VelocityCompatProxyServer(server);
		this.logger = new slf4jCompatLogger(logger);

		this.metricsFactory = metricsFactory;

		this.velocity_server = server;
		this.data_dir = data_dir.toFile();
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

		velocity_server.getChannelRegistrar().register(CHANNEL);

		if (!config.multiplexer_by_port.isEmpty()) {
			try {
				start_listeners();
			} catch (Exception e) {
				get_logger().log(Level.SEVERE, "Failed to initialize login session injector!", e);
			}
		}
	}

	private void start_listeners() {
		get_logger().log(Level.INFO, "Attempting to register auth multiplexers");

		final var server = (VelocityServer) velocity_server;
		ConnectionManager cm;
		try {
			final var velocity_server = Class.forName("com.velocitypowered.proxy.VelocityServer");
			final var cm_field = velocity_server.getDeclaredField("cm");
			cm_field.setAccessible(true);
			cm = (ConnectionManager) cm_field.get(server);
		} catch (
				ClassNotFoundException
				| NoSuchFieldException
				| SecurityException
				| IllegalArgumentException
				| IllegalAccessException e
		) {
			e.printStackTrace();
			return;
		}

		for (final var multiplexer_map : config.multiplexer_by_port.entrySet()) {
			final var port = multiplexer_map.getKey();
			final var id = multiplexer_map.getValue();

			get_logger().log(Level.INFO, "Registering multiplexer ID " + id + ", bound to port " + port);

			final var address = new InetSocketAddress(port);
			cm.bind(address);
		}
	}

	@Subscribe
	public void on_disable(final ProxyShutdownEvent event) {
		disable();
	}

	private void disable() {
		velocity_server.getEventManager().unregisterListeners(this);

		velocity_server.getChannelRegistrar().unregister(CHANNEL);

		server = null;
		logger = null;
	}

	@Override
	public File get_data_folder() {
		return this.data_dir;
	}

	@Override
	public org.oddlama.vane.proxycore.ProxyServer get_proxy() {
		return server;
	}

	public ProxyServer get_raw_proxy() {
		return velocity_server;
	}

}
