package org.oddlama.vane.regions.region;

import org.oddlama.vane.regions.Regions;

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
    VINE_GROWTH(false);

    private boolean def;

    private EnvironmentSetting(final boolean def) {
        this.def = def;
    }

    public boolean default_value() {
        return def;
    }

    public boolean has_override() {
        return get_override() != 0;
    }

    public int get_override() {
        return Regions.environment_overrides.get_override(this);
    }
}
