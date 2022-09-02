package org.oddlama.vane.proxycore.config;

import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.proxycore.VaneProxyPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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

	public boolean load() {
		final var file = file();
		if (!file.exists()) {
			save_default(file);
		}

		Config parsed_config;
		try {
			parsed_config = new Config(file());
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

	public void save_default(final File file) {
		file.getParentFile().mkdirs();
		final var content = """
				[auth_multiplex]
				   \s
				    # Define your auth multiplexers
				    # These allow the specified UUIDs to join the server
				    # multiple times with a fake UUID. IDs must be > 0.
				   \s
				    # Example:
				    #
				    # [auth_multiplex.ID_NUMBER]
				    # port = 25566
				    # allowed_uuids = ["b4a6717f-e3ab-4348-8e18-022827ef3177"]
				    #
				    # (Where ID_NUMBER is a nonzero integer)
				    #
				    # This will allow *only* UUID "b4a6717f-e3ab-4348-8e18-022827ef3177"
				    # to use this multiplexer. You can add as many UUIDs as you want.
				    #
				    # To make it public, simply leave out the `allowed_uuids` field.
				    #
				    # It is *not* possible to have multiple multiplexers on the same port.
				   \s
				[managed_servers]

				    # Define your managed servers
				    #
				    # NOTE: No field is necessary! You can leave out anything you don't
				    #       want, including entire sections!

				    # Example:
				    #
				    #   [managed_servers.my_server]
				    #
				    #   # The server display name (used for messages and substitutions)
				    #
				    #   display_name = "§6> §6§lMy Minecraft Server§6 <"
				    #
				    #       # The online configuration, things to show in the server list
				    #       # when the server is online
				    #
				    #       [managed_servers.my_server.online]
				    #
				    #       # The server favicon (absolute path or relative to proxy directory)
				    #       favicon = "../%SERVER%/server-icon.png"
				    #
				    #       # The motd for the server list (up to two lines)
				    #       motd = "%SERVER_DISPLAY_NAME%\\n§aowo§7 | %QUOTE%"
				    #
				    #       # A list of quotes for substitutions
				    #       quotes = ["Ah, yes."]
				    #
				    #       # The offline configuration, for when the server is offline
				    #
				    #       [managed_servers.my_server.offline]
				    #       favicon = "../%SERVER%/server-icon-offline.png"
				    #       motd = "%SERVER_DISPLAY_NAME%\\n§cowo§7 | %QUOTE%"
				    #       quotes = ["Paniik!"]
				    #
				    #       # Automatic start settings
				    #       [managed_servers.my_server.start]
				    #
				    #       # The command to execute when the first player joins while the server is offline
				    #       # %SERVER%: The server identifier
				    #
				    #       cmd = ["/path/to/startscript", "arg1", "will also substitute %SERVER%"]
				    #
				    #       # The kick message to send to the player that started the server
				    #       # %SERVER%: The server identifier
				    #       # %SERVER_DISPLAY_NAME%: The server display name
				    #
				    #       kick_msg = "%SERVER_DISPLAY_NAME% §7is being started.\\n§7Try again in §b10 Seconds§7 \\\\(^-^)/"
				""";

		// Save content to file
		try {
			Files.writeString(file.toPath(), content);
		} catch (IOException e) {
			plugin.get_logger().log(Level.SEVERE, "Error while writing config file '" + file + "'", e);
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
