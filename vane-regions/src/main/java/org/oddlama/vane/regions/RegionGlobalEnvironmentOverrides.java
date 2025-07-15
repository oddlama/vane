package org.oddlama.vane.regions;

import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.region.EnvironmentSetting;

public class RegionGlobalEnvironmentOverrides extends ModuleComponent<Regions> {

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether animals can spawn.")
    public int config_animals;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether monsters can spawn.")
    public int config_monsters;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether explosions can happen.")
    public int config_explosions;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether fire spreads and consumes.")
    public int config_fire;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether pvp is allowed.")
    public int config_pvp;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether fields can be trampled.")
    public int config_trample;

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether vines can grow.")
    public int config_vine_growth;

    public RegionGlobalEnvironmentOverrides(Context<Regions> context) {
        super(
            context.namespace(
                "global_environment_overrides",
                "This controls global environment setting overrides for all regions on the server. `0` means no-override, the player-configured values are used normally, `1` force-enables this setting for all regions, `-1` force-disables respectively."
            )
        );
    }

    public int get_override(final EnvironmentSetting setting) {
        switch (setting) {
            case ANIMALS:
                return config_animals;
            case MONSTERS:
                return config_monsters;
            case EXPLOSIONS:
                return config_explosions;
            case FIRE:
                return config_fire;
            case PVP:
                return config_pvp;
            case TRAMPLE:
                return config_trample;
            case VINE_GROWTH:
                return config_vine_growth;
        }
        return 0;
    }

    @Override
    public void on_enable() {}

    @Override
    public void on_disable() {}
}
