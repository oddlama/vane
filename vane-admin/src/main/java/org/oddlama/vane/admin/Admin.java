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
	// Persistent storage
	@Persistent
	public PersistentLocation storage_spawn_location = null;

	public Admin() {
		// Create components
		new org.oddlama.vane.admin.commands.Gamemode(this);
		new org.oddlama.vane.admin.commands.Setspawn(this);
		new org.oddlama.vane.admin.commands.Spawn(this);
		new org.oddlama.vane.admin.commands.Time(this);
		new org.oddlama.vane.admin.commands.Weather(this);

		var autostop_group = new AutostopGroup(this);
		new AutostopListener(autostop_group);
		new org.oddlama.vane.admin.commands.Autostop(autostop_group);

		new WorldProtection(this);
		new HazardProtection(this);
		new ResourcePackDistributor(this);
		new ChatMessageFormatter(this);
	}
}
