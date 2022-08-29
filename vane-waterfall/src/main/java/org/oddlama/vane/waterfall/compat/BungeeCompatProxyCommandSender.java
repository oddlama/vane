package org.oddlama.vane.waterfall.compat;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import org.oddlama.vane.proxycore.commands.ProxyCommandSender;

public class BungeeCompatProxyCommandSender implements ProxyCommandSender {

	CommandSender sender;

	public BungeeCompatProxyCommandSender(CommandSender sender) {
		this.sender = sender;
	}

	@Override
	public void send_message(String message) {
		sender.sendMessage(TextComponent.fromLegacyText(message));
	}

}
