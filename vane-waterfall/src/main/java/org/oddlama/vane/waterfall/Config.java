package org.oddlama.vane.waterfall;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class Config {

	private final Waterfall plugin;

	// name → managed server
	public final Map<String, ManagedServer> managed_servers = new HashMap<>();
	// port → alias id (starts at 1)
	public final Map<Integer, Integer> multiplexer_by_port = new HashMap<>();

	public Config(final Waterfall plugin) {
		this.plugin = plugin;
	}

	private File file() {
		return new File(plugin.getDataFolder(), "config.yml");
	}

	public void load() {
		final var file = file();
		if (!file.exists()) {
			save_default(file);
		}

		try {
			final var conf = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

			final var conf_auth_multiplex = conf.getSection("auth_multiplex");
			for (final var key : conf_auth_multiplex.getKeys()) {
				multiplexer_by_port.put(Integer.parseInt(key), conf_auth_multiplex.getInt(key));
			}

			final var conf_managed_servers = conf.getSection("managed_servers");
			for (final var id : conf_managed_servers.getKeys()) {
				final var section = conf_managed_servers.getSection(id);
				final var display_name = section.getString("display_name");
				final var favicon = section.getString("favicon");
				final var conf_quotes = section.getSection("quotes");
				final var quotes_online = conf_quotes.getStringList("online");
				final var quotes_offline = conf_quotes.getStringList("offline");
				final var conf_motd = section.getSection("motd");
				final var motd_online = conf_motd.getString("online");
				final var motd_offline = conf_motd.getString("offline");
				final var conf_start = section.getSection("start");
				final var start_cmd = conf_start.getStringList("cmd");
				final var start_kick_msg = conf_start.getString("kick_msg");

				final var managed_server = new ManagedServer(id);
				managed_server.display_name(display_name);
				managed_server.favicon(favicon);
				managed_server.quotes_online(quotes_online);
				managed_server.quotes_offline(quotes_offline);
				managed_server.motd_online(motd_online);
				managed_server.motd_offline(motd_offline);
				managed_server.start_cmd(start_cmd);
				managed_server.start_kick_msg(start_kick_msg);
				managed_servers.put(id, managed_server);
			}
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Error while loading config file '" + file + "'", e);
		}
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
			Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Error while writing config file '" + file + "'", e);
		}
	}
}
