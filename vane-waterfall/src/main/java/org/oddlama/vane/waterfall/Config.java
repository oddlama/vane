package org.oddlama.vane.waterfall;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.logging.Level;
import java.io.File;
import java.io.IOException;
import java.lang.StringBuilder;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.UUID;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.config.ConfigurationProvider;

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
				final var conf_motd = section.getSection("motd");
				final var motd_online = conf_motd.getString("online");
				final var motd_offline = conf_motd.getString("offline");
				final var conf_start = section.getSection("start");
				final var start_cmd = conf_start.getStringList("cmd");
				final var start_kick_msg = conf_start.getString("kick_msg");

				final var managed_server = new ManagedServer(id);
				managed_server.display_name(display_name);
				managed_server.favicon(favicon);
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
		final var content = "# vim: set tabstop=2 softtabstop=0 expandtab shiftwidth=2:\n"
			+ "\n"
			+ "# A mapping of <port>: <multiplexer_id>\n"
			+ "# Allows players with the permission 'vane_waterfall.multiplex_auth.<multiplexer_id>'\n"
			+ "# to connect to the same server multiple times with a fake uuid. Multiplexer id's must be > 0.\n"
			+ "# Default:\n"
			+ "# auth_multiplex: {}\n"
			+ "auth_multiplex: {}\n"
			+ "\n"
			+ "# A dictionary of managed servers, which will be started on demand.\n"
			+ "# Example:\n"
			+ "# my_server:\n"
			+ "#   display_name: \"My Minecraft Server\"\n"
			+ "#   # %SERVER%: The server identifier\n"
			+ "#   favicon: \"../%SERVER%/favicon.png\"\n"
			+ "#   motd:\n"
			+ "#     # %SERVER_DISPLAY_NAME%: The server display name\n"
			+ "#     online: \"§f>> §3%SERVER_DISPLAY_NAME%§f <<\\n\\\n"
			+ "#             §aowo\"\n"
			+ "#     # %SERVER_DISPLAY_NAME%: The server display name\n"
			+ "#     offline: \"§f>> §3%SERVER_DISPLAY_NAME%§f <<\\n\\\n"
			+ "#             §cowo\"\n"
			+ "#   start:\n"
			+ "#     # %SERVER%: The server identifier\n"
			+ "#     cmd: [\"/path/to/startscript\", \"arg1\", \"will also subsitute %SERVER%\"]\n"
			+ "#     # %SERVER%: The server identifier\n"
			+ "#     # %SERVER_DISPLAY_NAME%: The server display name\n"
			+ "#     kick_msg: \"§f>> §3%SERVER_DISPLAY_NAME%§f << §7is being started.\\n\\\n"
			+ "#               §7Try again in §b10 Seconds§7 \\\\(^-^)/\"\n"
			+ "# Default:\n"
			+ "# managed_servers: {}\n"
			+ "managed_servers: {}\n"
			;

		// Save content to file
		try {
			Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Error while writing config file '" + file + "'", e);
		}
	}
}
