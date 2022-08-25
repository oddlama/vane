package org.oddlama.vane.proxycore;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

@SuppressWarnings("unused")
public class ManagedServer {

	private String id;
	public String display_name;
	public String favicon;
	public Quotes quotes;
	public Motd motd;
	public ServerStart start;

	public void id(String id) {
		this.id = id;
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
		return motd.online.replace("%SERVER_DISPLAY_NAME%", display_name).replace("%QUOTE%", random_quote_online());
	}

	public String motd_offline() {
		if (motd.offline == null) {
			return "";
		}
		return motd.offline.replace("%SERVER_DISPLAY_NAME%", display_name).replace("%QUOTE%", random_quote_offline());
	}

	public File favicon_file() {
		return new File(favicon.replace("%SERVER%", id));
	}

	public String[] start_cmd() {
		if (start.cmd == null) {
			return null;
		}
		return Arrays.stream(start.cmd).map(s -> s.replace("%SERVER%", id)).toArray(String[]::new);
	}

	public String start_kick_msg() {
		return start.kick_msg.replace("%SERVER%", id).replace("%SERVER_DISPLAY_NAME%", display_name);
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
