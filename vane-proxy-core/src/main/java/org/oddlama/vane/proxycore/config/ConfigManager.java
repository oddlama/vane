package org.oddlama.vane.proxycore.config;

import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.logging.Level;

public class ConfigManager {

	// name → managed server
	public final Map<String, ManagedServer> managed_servers = new HashMap<>();
	// port → alias id (starts at 1)
	public final Map<Integer, AuthMultiplex> multiplexer_by_id = new HashMap<>();
	private final VaneProxyPlugin plugin;

	public ConfigManager(final VaneProxyPlugin plugin) {
		this.plugin = plugin;
	}

	private File file() {
		return new File(plugin.get_data_folder(), "config.toml");
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public boolean load() {
		final var file = file();
		if (!file.exists() && !save_default(file)) {
			plugin.get_logger().log(Level.SEVERE, "Unable to create default config! Bailing.");
			return false;
		}

		Config parsed_config;
		try {
			parsed_config = new Config(file);
		} catch (Exception e) {
			plugin.get_logger().log(Level.SEVERE, "Error while loading config file '" + file + "'", e);
			return false;
		}

		if (parsed_config.auth_multiplex.containsKey(0)) {
			plugin.get_logger().log(Level.SEVERE, "Attempted to register a multiplexer with id 0!");
			return false;
		}

		// Make sure there are no duplicate ports
		Set<Integer> registered_ports = new HashSet<>();
		for (final var multiplexer : parsed_config.auth_multiplex.values()) {
			registered_ports.add(multiplexer.port);
		}

		if (parsed_config.auth_multiplex.size() != registered_ports.size()) {
			plugin.get_logger().log(Level.SEVERE, "Attempted to register multiple multiplexers on the same port!");
			return false;
		}

		multiplexer_by_id.putAll(parsed_config.auth_multiplex);
		managed_servers.putAll(parsed_config.managed_servers);

		return true;
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public boolean save_default(final File file) {
		try {
			file.getParentFile().mkdirs();
			Files.copy(
					Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.toml")),
					file.toPath(),
					StandardCopyOption.REPLACE_EXISTING
			);
			return true;
		} catch (Exception e) {
			plugin.get_logger().log(Level.SEVERE, "Error while writing config file '" + file + "'", e);
			return false;
		}
	}

	@Nullable
	public Map.Entry<Integer, AuthMultiplex> get_multiplexer_for_port(Integer port) {
		for (final var multiplexer : multiplexer_by_id.entrySet()) {
			// We already checked there are no duplicate ports when parsing
			if (Objects.equals(multiplexer.getValue().port, port)) {
				return multiplexer;
			}
		}

		return null;
	}

}
