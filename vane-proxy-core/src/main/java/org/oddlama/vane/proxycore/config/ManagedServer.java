package org.oddlama.vane.proxycore.config;

import com.electronwill.nightconfig.core.CommentedConfig;
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
	public Quotes quotes;
	public Motd motd;
	public ServerStart start;
	@Nullable
	private String encoded_favicon;

	public ManagedServer(String id,
						 String display_name,
						 @Nullable String favicon,
						 CommentedConfig quotes,
						 CommentedConfig motd,
						 CommentedConfig start) throws IOException {
		this.display_name = display_name;

		// Replaces placeholders in messages
		this.quotes = new Quotes(id, display_name, quotes);
		this.motd = new Motd(id, display_name, motd);
		this.start = new ServerStart(id, display_name, start);

		if (favicon == null || favicon.isEmpty()) {
			return;
		}

		// Try and encode the favicon
		File favicon_file = new File(favicon.replace("%SERVER%", id));
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

		this.encoded_favicon = encoded_favicon;
	}

	public String favicon() {
		return encoded_favicon;
	}

	public String[] start_cmd() {
		return start.cmd;
	}

	public String start_kick_msg() {
		return start.kick_msg;
	}

	public String random_quote(QuoteSource source) {
		final String[] quote_set;
		switch (source) {
			case ONLINE -> quote_set = quotes.online;
			case OFFLINE -> quote_set = quotes.offline;
			default -> {
				return "";
			}
		}

		if (quote_set == null || quote_set.length == 0) {
			return "";
		}
		return quote_set[new Random().nextInt(quote_set.length)];
	}

	public String motd(MotdSource source) {
		final String sourced_motd;
		final QuoteSource quote_source;
		switch (source) {
			case ONLINE -> {
				sourced_motd = motd.online;
				quote_source = QuoteSource.ONLINE;
			}
			case OFFLINE -> {
				sourced_motd = motd.offline;
				quote_source = QuoteSource.OFFLINE;
			}
			default -> {
				return "";
			}
		}

		if (sourced_motd == null) {
			return "";
		}
		return sourced_motd.replace("%QUOTE%", random_quote(quote_source));
	}

	public enum QuoteSource {
		ONLINE,
		OFFLINE,
	}

	public enum MotdSource {
		ONLINE,
		OFFLINE,
	}

	private static class Quotes {

		public String[] online;
		public String[] offline;

		public Quotes(String id, String display_name, CommentedConfig config) {
			List<String> offline = config.get("offline");
			List<String> online = config.get("online");

			if (offline != null)
				this.offline = offline.stream().map(s -> s.replace("%SERVER%", id)).toArray(String[]::new);
			else this.offline = null;

			if (online != null)
				this.online = online.stream().map(s -> s.replace("%SERVER%", id).replace("%SERVER_DISPLAY_NAME%", display_name)).toArray(String[]::new);
			else this.online = null;
		}

	}

	private static class Motd {

		public String online;
		public String offline;

		public Motd(String id, String display_name, CommentedConfig config) {
			var offline = config.get("offline");
			var online = config.get("online");

			if (!(offline == null || offline instanceof String))
				throw new IllegalArgumentException("Managed server '" + id + "' has an invalid offline MOTD!");

			if (!(online == null || online instanceof String))
				throw new IllegalArgumentException("Managed server '" + id + "' has an invalid online MOTD!");

			if (online != null) this.online = ((String) online).replace("%SERVER_DISPLAY_NAME%", display_name);
			else this.online = null;

			if (offline != null) this.offline = ((String) offline).replace("%SERVER_DISPLAY_NAME%", display_name);
			else this.offline = null;
		}

	}

	private static class ServerStart {

		public String[] cmd;
		public String kick_msg;

		public ServerStart(String id, String display_name, CommentedConfig config) {
			List<String> cmd = config.get("cmd");
			var kick_msg = config.get("kick_msg");

			if (!(kick_msg == null || kick_msg instanceof String))
				throw new IllegalArgumentException("Managed server '" + id + "' has invalid kick message!");

			if (cmd != null) this.cmd = cmd.stream().map(s -> s.replace("%SERVER%", id)).toArray(String[]::new);
			else this.cmd = null;

			if (kick_msg != null)
				this.kick_msg = ((String) kick_msg).replace("%SERVER%", id).replace("%SERVER_DISPLAY_NAME%", display_name);
			else this.kick_msg = null;
		}

	}

}
