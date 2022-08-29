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
	@Nullable
	private String encoded_favicon;
	@Nullable
	private String favicon;
	public Quotes quotes;
	public Motd motd;
	public ServerStart start;

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

	public String random_quote_online() {
		String[] quotes_online = quotes.online;
		if (quotes_online == null || quotes_online.length == 0) {
			return "";
		}
		return quotes_online[new Random().nextInt(quotes_online.length)];
	}

	public String random_quote_offline() {
		String[] quotes_offline = quotes.offline;
		if (quotes_offline == null || quotes_offline.length == 0) {
			return "";
		}
		return quotes_offline[new Random().nextInt(quotes_offline.length)];
	}

	public String motd_online() {
		if (motd.online == null) {
			return "";
		}
		return motd.online.replace("%QUOTE%", random_quote_online());
	}

	public String motd_offline() {
		if (motd.offline == null) {
			return "";
		}
		return motd.offline.replace("%QUOTE%", random_quote_offline());
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
