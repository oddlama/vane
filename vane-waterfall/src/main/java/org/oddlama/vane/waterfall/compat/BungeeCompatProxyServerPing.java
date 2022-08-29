package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import org.oddlama.vane.proxycore.listeners.ProxyServerPing;

public class BungeeCompatProxyServerPing implements ProxyServerPing {

	ServerPing ping;

	public BungeeCompatProxyServerPing(ServerPing ping) {
		this.ping = ping;
	}

	@Override
	public void set_description(String description) {
		ping.setDescriptionComponent(new TextComponent(TextComponent.fromLegacyText(description)));
	}

	@Override
	public void set_favicon(String encoded_favicon) {
		ping.setFavicon(encoded_favicon);
	}

}
