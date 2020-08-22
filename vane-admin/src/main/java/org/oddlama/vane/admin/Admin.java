package org.oddlama.vane.admin;

import org.bukkit.event.Listener;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleGroup;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.oddlama.vane.util.Message;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@VaneModule("admin")
public class Admin extends Module<Admin> {
	// Configuration
	@ConfigVersion(1)
	public long config_version;

	// Language
	@LangVersion(1)
	public long lang_version;

	@LangMessage
	private Message lang_player_chat_format;
	@LangMessage
	private Message lang_player_join;
	@LangMessage
	private Message lang_player_kick;
	@LangMessage
	private Message lang_player_quit;

	public Admin() {
		// Create components
		new org.oddlama.vane.admin.commands.Setspawn(this);
		new org.oddlama.vane.admin.commands.Spawn(this);

		var autostop_group = new AutostopGroup(this);
		new AutostopListener(autostop_group);
		new org.oddlama.vane.admin.commands.Autostop(autostop_group);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_chat(AsyncPlayerChatEvent event) {
		// TODO color based on privilege or config value.... somehow
		// link permission groups to different config values....
		String color = "§a";
		event.setFormat(lang_player_chat_format.format(color, "%1$s", "%2$s"));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void on_player_join(PlayerJoinEvent event) {
		event.setJoinMessage(lang_player_join.format(event.getPlayer().getPlayerListName()));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_kick(PlayerKickEvent event) {
		event.setLeaveMessage(lang_player_kick.format(event.getPlayer().getPlayerListName()));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_quit(PlayerQuitEvent event) {
		event.setQuitMessage(lang_player_quit.format(event.getPlayer().getPlayerListName()));
	}
}
