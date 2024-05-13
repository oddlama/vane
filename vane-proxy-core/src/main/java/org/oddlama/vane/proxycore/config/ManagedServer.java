package org.oddlama.vane.proxycore.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Random;

public class ManagedServer {

	public String display_name;
	public ServerStart start;

	private final String id;
	private final StatefulConfiguration online_config;
	private final StatefulConfiguration offline_config;

	public ManagedServer(String id,
						 String display_name,
						 CommentedConfig online_config_section,
						 CommentedConfig offline_config_section,
						 CommentedConfig start) throws IOException {
		this.id = id;
		this.display_name = display_name;

		this.online_config = new StatefulConfiguration(id, display_name, online_config_section);
		this.offline_config = new StatefulConfiguration(id, display_name, offline_config_section);
		this.start = new ServerStart(id, display_name, start);
	}

	public @NotNull String id() {
		return this.id;
	}

	public @Nullable String favicon(ConfigItemSource source) {
		switch (source) {
			case ONLINE -> {
				return online_config.encoded_favicon;
			}
			case OFFLINE -> {
				return offline_config.encoded_favicon;
			}
			default -> {
				return null;
			}
		}
	}

	public String[] start_cmd() {
		return start.cmd;
	}

	public String start_kick_msg() {
		return start.kick_msg;
	}

	private String random_quote(ConfigItemSource source) {
		final String[] quote_set;
		switch (source) {
			case ONLINE -> quote_set = online_config.quotes;
			case OFFLINE -> quote_set = offline_config.quotes;
			default -> {
				return "";
			}
		}

		if (quote_set == null || quote_set.length == 0) {
			return "";
		}
		return quote_set[new Random().nextInt(quote_set.length)];
	}

	public String motd(ConfigItemSource source) {
		final String sourced_motd;
		final ConfigItemSource quote_source;
		switch (source) {
			case ONLINE -> {
				sourced_motd = online_config.motd;
				quote_source = ConfigItemSource.ONLINE;
			}
			case OFFLINE -> {
				sourced_motd = offline_config.motd;
				quote_source = ConfigItemSource.OFFLINE;
			}
			default -> {
				return "";
			}
		}

		if (sourced_motd == null) {
			return "";
		}
		return sourced_motd.replace("{QUOTE}", random_quote(quote_source));
	}

	public Integer command_timeout() {
		return start.timeout;
	}

	public enum ConfigItemSource {
		ONLINE,
		OFFLINE,
	}

	private static class StatefulConfiguration {

		public String[] quotes = null;
		public String motd = null;
		private @Nullable String encoded_favicon;

		public StatefulConfiguration(String id, String display_name, CommentedConfig config) throws IOException {
			// [managed_servers.my_server.state]
			if (config == null) {
				// The whole section is missing
				return;
			}

			// quotes = ["", ...]
			List<String> quotes = config.get("quotes");

			if (quotes != null)
				this.quotes = quotes.stream()
						.filter(s -> !s.isBlank())
						.map(s -> s.replace("{SERVER}", id)
								.replace("{SERVER_DISPLAY_NAME}", display_name))
						.toArray(String[]::new);

			// motd = "..."
			var motd = config.get("motd");

			if (!(motd == null || motd instanceof String))
				throw new IllegalArgumentException("Managed server '" + id + "' has a non-string MOTD!");

			if (motd != null && !((String) motd).isEmpty())
				this.motd = ((String) motd).replace("{SERVER_DISPLAY_NAME}", display_name);

			// favicon = "..."
			var favicon_path = config.get("favicon");

			if (!(favicon_path == null || favicon_path instanceof String))
				throw new IllegalArgumentException("Managed server '" + id + "' has a non-string favicon path!");

			if (favicon_path != null && !((String) favicon_path).isEmpty())
				this.encoded_favicon = encode_favicon(id, (String) favicon_path);
		}

		private static String encode_favicon(String id, String favicon_path) throws IOException {
			File favicon_file = new File(favicon_path.replace("{SERVER}", id));
			BufferedImage image;
			try {
				image = ImageIO.read(favicon_file);
			} catch (IOException e) {
				throw new IOException("Failed to read favicon file: " + e.getMessage());
			}


			if (image.getWidth() != 64 || image.getHeight() != 64) {
				throw new IllegalArgumentException("Favicon has invalid dimensions (must be 64x64)");
			}

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			ImageIO.write(image, "PNG", stream);
			byte[] favicon_bytes = stream.toByteArray();

			String encoded_favicon = "data:image/png;base64," + Base64.getEncoder().encodeToString(favicon_bytes);

			if (encoded_favicon.length() > Short.MAX_VALUE) {
				throw new IllegalArgumentException("Favicon file too large for server to process");
			}

			return encoded_favicon;
		}

	}

	public static class ServerStart {

		private static final int DEFAULT_TIMEOUT_SECONDS = 10;
		public String[] cmd;
		public Integer timeout;
		public String kick_msg;
		public boolean allow_anyone;

		public ServerStart(String id, String display_name, CommentedConfig config) {
			List<String> cmd = config.get("cmd");
			var timeout = config.get("timeout");
			var kick_msg = config.get("kick_msg");
			var allow_anyone = config.get("allow_anyone");

			if (cmd != null) this.cmd = cmd.stream().map(s -> s.replace("{SERVER}", id)).toArray(String[]::new);
			else this.cmd = null;

			if (!(kick_msg == null || kick_msg instanceof String))
				throw new IllegalArgumentException("Managed server '" + id + "' has an invalid kick message!");

			if (kick_msg != null)
				this.kick_msg = ((String) kick_msg).replace("{SERVER}", id).replace("{SERVER_DISPLAY_NAME}", display_name);
			else this.kick_msg = null;

			if (allow_anyone != null)
				this.allow_anyone = (boolean)allow_anyone;
			else this.allow_anyone = false;

			if (timeout == null) {
				this.timeout = DEFAULT_TIMEOUT_SECONDS;
				return;
			}

			if (!(timeout instanceof Integer))
				throw new IllegalArgumentException("Managed server '" + id + "' has an invalid command timeout!");

			this.timeout = (Integer) timeout;
		}

	}

}
