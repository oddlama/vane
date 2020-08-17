package org.oddlama.imex.core;

import org.oddlama.imex.annotation.ImexModule;
import org.oddlama.imex.annotation.ConfigVersion;
import org.oddlama.imex.annotation.LangVersion;

@ImexModule
public class Core extends Module {
	// "DO NOT CHANGE! The version of this config file. Used to determine if the config needs to be updated."
	@ConfigVersion(1)
	public long config_version;

	@LangVersion(1)
	public long lang_version;

	@Override
	public void onEnable() {
		super.onEnable();

		// Connect to database

		// Enable dynmap integration

		// Register listeners

		// Register commands

		// Schedule shutdown if there are no players
	}

	@Override
	public void onDisable() {
		super.onDisable();

		// Unregister commands

		// Unregister listeners

		// Disable dynmap integration

		// Close database
	}
}
