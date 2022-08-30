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
import org.bstats.velocity.Metrics;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.proxycore.log.slf4jCompatLogger;
import org.oddlama.vane.util.Version;
import org.oddlama.velocity.compat.VelocityCompatProxyServer;
import org.oddlama.velocity.listeners.ProxyDisconnectListener;
import org.oddlama.velocity.listeners.ProxyLoginListener;
import org.oddlama.velocity.listeners.ProxyPreLoginListener;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Plugin(id = "vane-velocity", name = "Vane Velocity", version = Version.VERSION, description = "TODO",
		authors = {"oddlama", "Serial-ATA"}, url = "https://github.com/oddlama/vane")
public class Velocity extends VaneProxyPlugin {

	public static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create(CHANNEL_AUTH_MULTIPLEX_NAMESPACE, CHANNEL_AUTH_MULTIPLEX_NAME);
	private final ProxyServer velocity_server;
	private final File data_dir;
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

		logger.info("Hello world!");
	}

	@Subscribe
	public void onEnable(final ProxyInitializeEvent event) {
		metricsFactory.make(this, 8891);

		EventManager event_manager = velocity_server.getEventManager();

		event_manager.register(this, new ProxyPreLoginListener(this));
		event_manager.register(this, new ProxyLoginListener(this));
		event_manager.register(this, new ProxyDisconnectListener(this));

		velocity_server.getChannelRegistrar().register(CHANNEL);
	}

	@Subscribe
	public void on_disable(final ProxyShutdownEvent event) {
		velocity_server.getEventManager().unregisterListeners(this);

		velocity_server.getChannelRegistrar().unregister(CHANNEL);
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
