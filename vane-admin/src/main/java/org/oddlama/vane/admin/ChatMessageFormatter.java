package org.oddlama.vane.admin;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import org.bukkit.craftbukkit.v1_16_R2.util.CraftChatMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;

public class ChatMessageFormatter extends Listener<Admin> {
	@LangMessage private TranslatedMessage lang_player_chat_format;
	@LangMessage private TranslatedMessage lang_player_join;
	@LangMessage private TranslatedMessage lang_player_kick;
	@LangMessage private TranslatedMessage lang_player_quit;

	public ChatMessageFormatter(Context<Admin> context) {
		super(context);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_chat(AsyncPlayerChatEvent event) {
		// TODO color based on privilege or config value.... somehow
		// link permission groups to different config values....
		final var color = "§b";
		event.setCancelled(true);

		// Convert IChatBaseComponent[] -> BaseComponent[]
		final var components = CraftChatMessage.fromString(event.getMessage());
		final var chat = new TextComponent();
		for (int i = 0; i < components.length; ++i) {
			chat.addExtra(ComponentSerializer.parse(CraftChatMessage.toJSON(components[i]))[0]);
		}

		lang_player_chat_format.broadcast_server_players(color + event.getPlayer().getDisplayName(), chat);
		System.out.println("[chat] " + lang_player_chat_format.str(color + event.getPlayer().getDisplayName(), event.getMessage()));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_join(final PlayerJoinEvent event) {
		event.setJoinMessage(null);
		lang_player_join.broadcast_server("§6" + event.getPlayer().getPlayerListName());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_kick(final PlayerKickEvent event) {
		// Bug in Spigot, doesn't actually do anything.
		// https://hub.spigotmc.org/jira/browse/SPIGOT-3034
		event.setLeaveMessage("");
		lang_player_kick.broadcast_server("§6" + event.getPlayer().getPlayerListName());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_quit(final PlayerQuitEvent event) {
		event.setQuitMessage(null);
		lang_player_quit.broadcast_server("§6" + event.getPlayer().getPlayerListName());
	}
}
