package org.oddlama.imex.core;

import org.oddlama.imex.annotation.ImexModule;

@ImexModule
public class Core extends Module {
	@Override
	public void onLoad() {
	}

	@Override
	public void onEnable() {
		// Connect to database

		// Enable dynmap integration

		// Register listeners

		// Register commands

		// Schedule shutdown if there are no players
	}

	@Override
	public void onDisable() {
		// Unregister commands

		// Unregister listeners

		// Disable dynmap integration

		// Close database
	}
}
