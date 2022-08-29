package org.oddlama.vane.proxycore.config;

import org.oddlama.vane.proxycore.ManagedServer;
import org.oddlama.vane.proxycore.VaneProxyPlugin;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ConfigManager {

	// name → managed server
	public final Map<String, ManagedServer> managed_servers = new HashMap<>();
	// port → alias id (starts at 1)
	public final Map<Integer, Integer> multiplexer_by_port = new HashMap<>();
	private final VaneProxyPlugin plugin;

	public ConfigManager(final VaneProxyPlugin plugin) {
		this.plugin = plugin;
	}

	private File file() {
		return new File(plugin.get_data_folder(), "config.yml");
	}

	public boolean load() {
		final var file = file();
		if (!file.exists()) {
			save_default(file);
		}

		Yaml yaml = new Yaml(new CustomClassLoaderConstructor(Config.class.getClassLoader()));
		try (FileInputStream is = new FileInputStream(file)) {
			Config conf = yaml.loadAs(is, Config.class);
			if (conf == null) {
				plugin.get_logger().log(Level.SEVERE, "Failed to parse config (is it missing anything?)");
				return false;
			}

			multiplexer_by_port.putAll(conf.auth_multiplex);
			managed_servers.putAll(conf.managed_servers);

			for (var entry : managed_servers.entrySet()) {
				final var server_id = entry.getKey();
				var server = entry.getValue();

				try {
					server.post_process(server_id);
				} catch (IOException e) {
					plugin.get_logger().log(Level.SEVERE, "Failed to read favicon! (is the path correct?)");
					e.printStackTrace();
					return false;
				} catch (IllegalArgumentException e) {
					plugin.get_logger().log(Level.SEVERE, "Failed to set favicon: " + e.getMessage());
					return false;
				}
			}
		} catch (IOException e) {
			plugin.get_logger().log(Level.SEVERE, "Error while loading config file '" + file + "'", e);
			return false;
		}

		return true;
	}

	public void save_default(final File file) {
		file.getParentFile().mkdirs();
		final var content =
				"# vim: set tabstop=2 softtabstop=0 expandtab shiftwidth=2:\n" +
						"\n" +
						"# A mapping of <port>: <multiplexer_id>\n" +
						"# Allows players with the permission 'vane_waterfall.auth_multiplexer.<multiplexer_id>'\n" +
						"# to connect to the same server multiple times with a fake uuid. Multiplexer id's must be > 0.\n" +
						"# Default:\n" +
						"# auth_multiplex: {}\n" +
						"auth_multiplex: {}\n" +
						"\n" +
						"# A dictionary of managed servers, which will be started on demand.\n" +
						"# Example:\n" +
						"# managed_servers:\n" +
						"#   # Name of server as defined in proxy's config.yml\n" +
						"#   my_server:\n" +
						"#     # The server display name (used for messages and substitutions)\n" +
						"#     display_name: \"§6> §6§lMy Minecraft Server§6 <\"\n" +
						"#     # The server favicon (absolute path or relative to proxy directory)\n" +
						"#     # %SERVER%: The server identifier\n" +
						"#     favicon: \"../%SERVER%/server-icon.png\"\n" +
						"#     # A list of quotes for substitutions\n" +
						"#     quotes:\n" +
						"#       online:\n" +
						"#         - \"Ah, yes.\"\n" +
						"#       offline:\n" +
						"#         - \"Paniik!\"\n" +
						"#     # The motd for the server list (up to two lines)\n" +
						"#     motd:\n" +
						"#       # %SERVER_DISPLAY_NAME%: The server display name\n" +
						"#       online: \"%SERVER_DISPLAY_NAME%\\n\\\n" +
						"#         §aowo§7 | %QUOTE%\"\n" +
						"#       # %SERVER_DISPLAY_NAME%: The server display name\n" +
						"#       offline: \"%SERVER_DISPLAY_NAME%\\n\\\n" +
						"#         §cowo§7 | %QUOTE%\"\n" +
						"#     # Automatic start settings\n" +
						"#     start:\n" +
						"#       # The command to execute when the first player joins while the server is offline\n" +
						"#       # %SERVER%: The server identifier\n" +
						"#       cmd: [\"/path/to/startscript\", \"arg1\", \"will also subsitute %SERVER%\"]\n" +
						"#       # The kick message to send to the player that started the server\n" +
						"#       # %SERVER%: The server identifier\n" +
						"#       # %SERVER_DISPLAY_NAME%: The server display name\n" +
						"#       kick_msg: \"%SERVER_DISPLAY_NAME% §7is being started.\\n\\\n" +
						"#         §7Try again in §b10 Seconds§7 \\\\(^-^)/\"\n" +
						"# Default:\n" +
						"# managed_servers: {}\n" +
						"managed_servers: {}\n";

		// Save content to file
		try {
			Files.writeString(file.toPath(), content);
		} catch (IOException e) {
			plugin.get_logger().log(Level.SEVERE, "Error while writing config file '" + file + "'", e);
		}
	}

}
