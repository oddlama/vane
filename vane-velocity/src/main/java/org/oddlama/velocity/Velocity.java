package org.oddlama.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.oddlama.vane.util.Version;
import org.oddlama.velocity.compat.VelocityCompatProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Plugin(id = "vane-velocity", name = "Vane Velocity", version = Version.VERSION, description = "TODO",
		authors = {"oddlama", "Serial-ATA"}, url = "https://github.com/oddlama/vane")
public class Velocity extends VaneProxyPlugin {

	private final File data_dir;
	// bStats
	@SuppressWarnings("unused")
	private final Metrics.Factory metricsFactory;

	@Inject
	public Velocity(ProxyServer server, Logger logger, Metrics.Factory metricsFactory, @DataDirectory final Path data_dir) {
		this.server = new VelocityCompatProxyServer(server);
		this.metricsFactory = metricsFactory;

		this.data_dir = data_dir.toFile();

		logger.info("Hello world!");
	}

	@Subscribe
	public void onEnable(final ProxyInitializeEvent event) {
		Metrics metrics = metricsFactory.make(this, 8891);
	}

	@Override
	public File get_data_folder() {
		return this.data_dir;
	}

	@Override
	public org.oddlama.vane.proxycore.ProxyServer get_proxy() {
		return server;
	}

}
