package org.oddlama.vane.admin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

import io.papermc.paper.event.player.AsyncChatEvent;

public class ChatMessageFormatter extends Listener<Admin> {
	@LangMessage private TranslatedMessage lang_player_chat_format;
	@LangMessage private TranslatedMessage lang_player_join;
	@LangMessage private TranslatedMessage lang_player_kick;
	@LangMessage private TranslatedMessage lang_player_quit;

	public ChatMessageFormatter(Context<Admin> context) {
		super(context.group("chat_message_formatter", "Enables custom formatting of chat messages like player chats and join / quit messages."));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_chat(AsyncChatEvent event) {
		// TODO color based on privilege or config value.... somehow
		// link permission groups to different config values....
		// TODO custom chat color event?
		event.setCancelled(true);

		final var chat = event.message();
		final var who = event.getPlayer().displayName().color(NamedTextColor.AQUA);
		lang_player_chat_format.broadcast_server_players(who, chat);
		System.out.println("[chat] " + lang_player_chat_format.str(who, chat));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_join(final PlayerJoinEvent event) {
		event.joinMessage(null);
		lang_player_join.broadcast_server(event.getPlayer().playerListName().color(NamedTextColor.GOLD));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_kick(final PlayerKickEvent event) {
		// Bug in Spigot, doesn't actually do anything. But fixed in Paper since 1.17.
		// https://hub.spigotmc.org/jira/browse/SPIGOT-3034
		event.leaveMessage(Component.text(""));
		// message is handeled in quit event
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_quit(final PlayerQuitEvent event) {
		event.quitMessage(null);
		if (event.getReason() == PlayerQuitEvent.QuitReason.KICKED) {
			lang_player_kick.broadcast_server(event.getPlayer().playerListName().color(NamedTextColor.GOLD));
		} else {
			lang_player_quit.broadcast_server(event.getPlayer().playerListName().color(NamedTextColor.GOLD));
		}
	}
}
