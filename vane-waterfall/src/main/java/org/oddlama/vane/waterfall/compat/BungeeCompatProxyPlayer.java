package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.oddlama.vane.proxycore.ProxyPlayer;

import java.util.UUID;

public class BungeeCompatProxyPlayer extends ProxyPlayer {

	public ProxiedPlayer player;

	public BungeeCompatProxyPlayer(ProxiedPlayer player) {
		this.player = player;
	}

	@Override
	public void disconnect(String message) {
		player.disconnect(TextComponent.fromLegacyText(message));
	}

	@Override
	public UUID get_unique_id() {
		return player.getUniqueId();
	}

	@Override
	public int get_ping() {
		return player.getPing();
	}

	@Override
	public void send_message(String message) {
		player.sendMessage(TextComponent.fromLegacyText(message));
	}

}
