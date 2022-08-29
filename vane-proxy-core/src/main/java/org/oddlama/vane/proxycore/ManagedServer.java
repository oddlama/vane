package org.oddlama.vane.proxycore;

import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

public class ManagedServer {

	public String display_name;
	public Quotes quotes;
	public Motd motd;
	public ServerStart start;
	@Nullable
	private String encoded_favicon;
	@Nullable
	private String favicon;

	@SuppressWarnings("unused")
	public void setFavicon(@Nullable String favicon_path) {
		this.favicon = favicon_path;
	}

	public void post_process(String id) throws IOException, IllegalArgumentException {
		// Replace placeholders in messages
		if (start.cmd != null) {
			start.cmd = Arrays.stream(start.cmd).map(s -> s.replace("%SERVER%", id)).toArray(String[]::new);
		}

		if (start.kick_msg != null) {
			start.kick_msg = start.kick_msg.replace("%SERVER%", id).replace("%SERVER_DISPLAY_NAME%", display_name);
		}

		if (motd.online != null) {
			motd.online = motd.online.replace("%SERVER_DISPLAY_NAME%", display_name);
		}

		if (motd.offline != null) {
			motd.offline = motd.offline.replace("%SERVER_DISPLAY_NAME%", display_name);
		}

		// Try and encode the favicon
		if (favicon == null || favicon.isEmpty()) {
			return;
		}

		File favicon_file = new File(favicon.replace("%SERVER%", id));
		BufferedImage image = ImageIO.read(favicon_file);

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

	}

	private static class Motd {

		public String online;
		public String offline;

	}

	private static class ServerStart {

		public String[] cmd;
		public String kick_msg;

	}

}
