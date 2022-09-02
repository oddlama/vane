package org.oddlama.velocity.compat;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.oddlama.vane.proxycore.commands.ProxyCommandSender;

public class VelocityCompatProxyCommandSender implements ProxyCommandSender {

	CommandSource sender;

	public VelocityCompatProxyCommandSender(CommandSource sender) {
		this.sender = sender;
	}

	@Override
	public void send_message(String message) {
		sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
	}

}
