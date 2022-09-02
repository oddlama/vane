package org.oddlama.velocity.compat;

import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import org.oddlama.vane.proxycore.ProxyPlayer;

import java.util.UUID;

public class VelocityCompatProxyPlayer implements ProxyPlayer {

	final Player player;

	public VelocityCompatProxyPlayer(Player player) {
		this.player = player;
	}

	@Override
	public void disconnect(String message) {
		player.disconnect(Component.text(message));
	}

	@Override
	public UUID get_unique_id() {
		return player.getUniqueId();
	}

	@Override
	public long get_ping() {
		return player.getPing();
	}

	@Override
	public void send_message(String message) {
		player.sendMessage(Component.text(message));
	}

}
