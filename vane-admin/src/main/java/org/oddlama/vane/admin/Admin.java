package org.oddlama.vane.admin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.persistent.PersistentLocation;
import org.oddlama.vane.util.Message;

@VaneModule(name = "admin", bstats = 8638, config_version = 1, lang_version = 1, storage_version = 1)
public class Admin extends Module<Admin> {
	// Language
	@LangMessage
	private Message lang_player_chat_format;
	@LangMessage
	private Message lang_player_join;
	@LangMessage
	private Message lang_player_kick;
	@LangMessage
	private Message lang_player_quit;

	// Persistent storage
	@Persistent
	public PersistentLocation storage_spawn_location = null;

	public Admin() {
		// Create components
		new org.oddlama.vane.admin.commands.Enchant(this);
		new org.oddlama.vane.admin.commands.Gamemode(this);
		new org.oddlama.vane.admin.commands.Setspawn(this);
		new org.oddlama.vane.admin.commands.Spawn(this);
		new org.oddlama.vane.admin.commands.Time(this);
		new org.oddlama.vane.admin.commands.Weather(this);

		var autostop_group = new AutostopGroup(this);
		new AutostopListener(autostop_group);
		new org.oddlama.vane.admin.commands.Autostop(autostop_group);

		new HazardProtection(this);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void on_player_chat(AsyncPlayerChatEvent event) {
		// TODO color based on privilege or config value.... somehow
		// link permission groups to different config values....
		String color = "§b";
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
