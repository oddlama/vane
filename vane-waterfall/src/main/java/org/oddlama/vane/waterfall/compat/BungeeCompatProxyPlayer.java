package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.oddlama.vane.proxycore.ProxyPlayer;

public class BungeeCompatProxyPlayer extends ProxyPlayer {
	public ProxiedPlayer player;

	public BungeeCompatProxyPlayer(ProxiedPlayer player) {
		this.player = player;
	}

	@Override
	public void disconnect(String message) {
		player.disconnect(message);
	}

}
