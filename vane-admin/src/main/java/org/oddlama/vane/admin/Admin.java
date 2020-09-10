package org.oddlama.vane.admin;

import org.bukkit.Location;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.module.Module;

@VaneModule(name = "admin", bstats = 8638, config_version = 1, lang_version = 1, storage_version = 1)
public class Admin extends Module<Admin> {
	// Persistent storage
	@Persistent
	public Location storage_spawn_location = null;

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
		new ChatMessageFormatter(this);
	}
}
