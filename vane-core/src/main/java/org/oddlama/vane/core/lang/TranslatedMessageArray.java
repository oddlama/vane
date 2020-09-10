package org.oddlama.vane.core.lang;

import net.md_5.bungee.api.chat.TranslatableComponent;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.World;
import org.bukkit.command.CommandSender;

import org.oddlama.vane.core.module.Module;

public class TranslatedMessageArray {
	private Module<?> module;
	private String key;
	private List<String> default_translation;

	public TranslatedMessageArray(final Module<?> module, final String key, final List<String> default_translation) {
		this.module = module;
		this.key = key;
		this.default_translation = default_translation;
	}

	public String key() { return key; }
	public List<String> str(Object... args) {
		try {
			final var list = new ArrayList<String>();
			for (final var s : default_translation) {
				list.add(String.format(s, args));
			}
			return list;
		} catch (Exception e) {
			throw new RuntimeException("Error while formatting message '" + key() + "'", e);
		}
	}

	public TranslatableComponent[] format(Object... args) {
		final var arr = new TranslatableComponent[default_translation.length];
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

	public void send_and_log(final CommandSender sender, Object... args) {
		module.log.info(str(args));

		// Also send to sender if it's not the console
		if (sender == null || sender == module.getServer().getConsoleSender()) {
			return;
		} else {
			sender.sendMessage(format(args));
		}
	}
}
