package org.oddlama.vane.regions.region;

public enum EnvironmentSetting {
	// Spawning
	ANIMALS(true),
	MONSTERS(false),

	// Hazards
	EXPLOSIONS(false),
	FIRE(false),
	PVP(true),

	// Environment
	TRAMPLE(false),
	VINE_GROWTH(false),
	;

	private boolean def;
	private EnvironmentSetting(final boolean def) {
		this.def = def;
	}

	public boolean default_value() {
		return def;
	}
}
