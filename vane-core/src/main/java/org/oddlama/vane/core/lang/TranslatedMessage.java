package org.oddlama.vane.core.lang;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

public class TranslatedMessage {
	private String key;
	private String default_translation;

	public TranslatedMessage(final String key, final String default_translation) {
		this.key = key;
		this.default_translation = default_translation;
	}

	public String key() { return key; }
	public String str(Object... args) {
		return String.format(default_translation, args);
	}

	public TranslatableComponent format(Object... args) {
		return new TranslatableComponent(key, args);
	}

	public void broadcast_server_players(Object... args) {
		final var component = format(args);
		for (var player : Bukkit.getOnlinePlayers()) {
			player.sendMessage(component);
		}
	}

	public void broadcast_server(Object... args) {
		final var component = format(args);
		for (var player : Bukkit.getOnlinePlayers()) {
			player.sendMessage(component);
		}
		Bukkit.getLogger().info("[broadcast] " + str(args));
	}

	public void broadcast_world(final World world, Object... args) {
		final var component = format(args);
		for (var player : world.getPlayers()) {
			player.sendMessage(component);
		}
	}

	public void send(final CommandSender sender, Object... args) {
		if (sender == null || sender == Bukkit.getConsoleSender()) {
			Bukkit.getLogger().info(str(args));
		} else {
			sender.sendMessage(format(args));
		}
	}
}
