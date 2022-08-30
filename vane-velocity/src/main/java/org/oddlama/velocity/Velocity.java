package org.oddlama.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.oddlama.vane.util.Version;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "vane-velocity", name = "Vane Velocity", version = Version.VERSION, description = "TODO",
		authors = {"oddlama", "Serial-ATA"}, url = "https://github.com/oddlama/vane")
public class Velocity {

	@Inject
	public Velocity(ProxyServer server, Logger logger, @DataDirectory final Path data_dir) {
		logger.info("Hello world!");
	}

}
