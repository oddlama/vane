package org.oddlama.vane.waterfall;

import java.io.File;
import java.util.List;
import java.util.Random;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class ManagedServer {

	private final String id;
	private String display_name;
	private List<String> quotes_online;
	private List<String> quotes_offline;
	private String motd_online;
	private String motd_offline;
	private String favicon;
	private List<String> start_cmd;
	private String start_kick_msg;

	public ManagedServer(final String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public void display_name(String display_name) {
		this.display_name = display_name;
	}

	public String display_name() {
		return display_name;
	}

	public String random_quote_online() {
		if (quotes_online == null || quotes_online.isEmpty()) {
			return "";
		}
		return quotes_online.get(new Random().nextInt(quotes_online.size()));
	}

	public void quotes_online(List<String> quotes_online) {
		this.quotes_online = quotes_online;
	}

	public String random_quote_offline() {
		if (quotes_offline == null || quotes_offline.isEmpty()) {
			return "";
		}
		return quotes_offline.get(new Random().nextInt(quotes_offline.size()));
	}

	public void quotes_offline(List<String> quotes_offline) {
		this.quotes_offline = quotes_offline;
	}

	public BaseComponent[] motd_online() {
		if (motd_online == null) {
			return new BaseComponent[0];
		}
		return TextComponent.fromLegacyText(
			motd_online.replace("%SERVER_DISPLAY_NAME%", display_name()).replace("%QUOTE%", random_quote_online())
		);
	}

	public void motd_online(String motd_online) {
		this.motd_online = motd_online;
	}

	public BaseComponent[] motd_offline() {
		if (motd_offline == null) {
			return new BaseComponent[0];
		}
		return TextComponent.fromLegacyText(
			motd_offline.replace("%SERVER_DISPLAY_NAME%", display_name()).replace("%QUOTE%", random_quote_offline())
		);
	}

	public void motd_offline(String motd_offline) {
		this.motd_offline = motd_offline;
	}

	public File favicon_file() {
		return new File(favicon.replace("%SERVER%", id()));
	}

	public void favicon(String favicon) {
		this.favicon = favicon;
	}

	public String[] start_cmd() {
		if (start_cmd == null) {
			return null;
		}
		return start_cmd.stream().map(s -> s.replace("%SERVER%", id())).toArray(String[]::new);
	}

	public void start_cmd(List<String> start_cmd) {
		this.start_cmd = start_cmd;
	}

	public BaseComponent[] start_kick_msg() {
		return TextComponent.fromLegacyText(
			start_kick_msg.replace("%SERVER%", id()).replace("%SERVER_DISPLAY_NAME%", display_name())
		);
	}

	public void start_kick_msg(String start_kick_msg) {
		this.start_kick_msg = start_kick_msg;
	}
}
