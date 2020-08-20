package org.oddlama.vane.admin;

import org.bukkit.event.Listener;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.module.ModuleGroup;
import org.oddlama.vane.admin.commands.CommandSetspawn;
import org.oddlama.vane.admin.commands.CommandSpawn;

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
}
