package org.oddlama.vane.admin;

import org.bukkit.event.Listener;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleGroup;
import org.oddlama.vane.admin.commands.CommandSetspawn;
import org.oddlama.vane.admin.commands.CommandSpawn;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

	public Admin() {
		// Create components
		new CommandSetspawn(this);
		new CommandSpawn(this);

		var autostop_group = group("autostop", "Enable automatic server stop after certain time without online players.");
		new AutostopListener(autostop_group);
		//new CommandAutostop(autostop_group);
	}

	//@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	//public static void on_player_join(PlayerJoinEvent event) {
	//	event.setJoinMessage(lang_player_join.format("%player%", event.getPlayer().getPlayerListName()));
	//}

	//@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	//public static void on_player_kick(PlayerKickEvent event) {
	//	event.setLeaveMessage(Configuration.CHAT_PLAYER_KICK.get().replace("%player%", event.getPlayer().getPlayerListName()));
	//}

	//@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	//public static void on_player_quit(PlayerQuitEvent event) {
	//	event.setQuitMessage(Configuration.CHAT_PLAYER_QUIT.get().replace("%player%", event.getPlayer().getPlayerListName()));
	//}
}
