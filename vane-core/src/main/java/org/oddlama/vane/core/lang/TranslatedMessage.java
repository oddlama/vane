package org.oddlama.vane.core.lang;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.oddlama.vane.core.module.Module;

public class TranslatedMessage {
	private Module<?> module;
	private String key;
	private String default_translation;

	public TranslatedMessage(final Module<?> module, final String key, final String default_translation) {
		this.module = module;
		this.key = key;
		this.default_translation = default_translation;
	}

	public String key() { return key; }
	public String str(Object... args) {
		try {
			return String.format(default_translation, args);
		} catch (Exception e) {
			throw new RuntimeException("Error while formatting message '" + key() + "'", e);
		}
	}

	public TranslatableComponent format(Object... args) {
		return new TranslatableComponent(key, args);
	}

	public void broadcast_server_players(Object... args) {
		final var component = format(args);
		for (var player : module.getServer().getOnlinePlayers()) {
			player.sendMessage(component);
		}
	}

	public void broadcast_server(Object... args) {
		final var component = format(args);
		for (var player : module.getServer().getOnlinePlayers()) {
			player.sendMessage(component);
		}
		module.log.info("[broadcast] " + str(args));
	}

	public void broadcast_world(final World world, Object... args) {
		final var component = format(args);
		for (var player : world.getPlayers()) {
			player.sendMessage(component);
		}
	}

	public void send(final CommandSender sender, Object... args) {
		if (sender == null || sender == module.getServer().getConsoleSender()) {
			module.log.info(str(args));
		} else {
			sender.sendMessage(format(args));
		}
	}
}
