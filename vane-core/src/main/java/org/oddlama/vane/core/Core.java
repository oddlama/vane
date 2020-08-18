package org.oddlama.vane.core;

import org.oddlama.vane.core.commands.CommandVane;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;

@VaneModule("core")
public class Core extends Module {
	@ConfigVersion(1)
	public long config_version;

	@LangVersion(1)
	public long lang_version;

	// Variables
	public CommandVane command_vane = new CommandVane(this);

	@Override
	protected void on_enable() {
		register_command(command_vane);
	}

	@Override
	protected void on_disable() {
		unregister_command(command_vane);
	}

	@Override
	protected void on_config_change() {
	}
}
