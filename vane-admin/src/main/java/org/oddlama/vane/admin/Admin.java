package org.oddlama.vane.admin;

import org.bukkit.event.Listener;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;

@VaneModule("admin")
public class Admin extends Module<Admin> {
	// Configuration
	@ConfigVersion(1)
	public long config_version;

	// Language
	@LangVersion(1)
	public long lang_version;

	// Variables
	private ModuleContext<Admin> autostop_context = group("autostop",
			"Enable automatic server stop after certain time without online players.");
	private AutoStopListener autostop_listener = new AutoStopListener(autostop_context);
	private CommandAutoStop autostop_command = new CommandAutoStop(autostop_context);
}
