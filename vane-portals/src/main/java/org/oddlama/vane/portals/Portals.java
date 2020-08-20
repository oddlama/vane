package org.oddlama.vane.portals;

import org.bukkit.event.Listener;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigVersion;
import org.oddlama.vane.annotation.lang.LangVersion;
import org.oddlama.vane.core.module.Module;

@VaneModule("portals")
public class Portals extends Module<Portals> implements Listener {
	// Configuration
	@ConfigVersion(1)
	public long config_version;

	// Language
	@LangVersion(1)
	public long lang_version;

	@Override
	public void on_enable() {
		register_listener(this);
	}

	@Override
	protected void on_disable() {
		unregister_listener(this);
	}

	@Override
	protected void on_config_change() {
	}
}
