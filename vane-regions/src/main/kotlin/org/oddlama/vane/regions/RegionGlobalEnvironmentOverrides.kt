package org.oddlama.vane.regions

import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import org.oddlama.vane.regions.region.EnvironmentSetting

/** Global environment-setting overrides applied across all regions. */
class RegionGlobalEnvironmentOverrides(context: Context<Regions?>) :
    ModuleComponent<Regions?>(
        context.namespace(
            "GlobalEnvironmentOverrides",
            "This controls global environment setting overrides for all regions on the server. `0` means no-override, the player-configured values are used normally, `1` force-enables this setting for all regions, `-1` force-disables respectively.",
        )
    ) {
    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether animals can spawn.")
    /** Override value for the `ANIMALS` environment setting. */
    var configAnimals: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether monsters can spawn.")
    /** Override value for the `MONSTERS` environment setting. */
    var configMonsters: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether explosions can happen.")
    /** Override value for the `EXPLOSIONS` environment setting. */
    var configExplosions: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether fire spreads and consumes.")
    /** Override value for the `FIRE` environment setting. */
    var configFire: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether pvp is allowed.")
    /** Override value for the `PVP` environment setting. */
    var configPvp: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether fields can be trampled.")
    /** Override value for the `TRAMPLE` environment setting. */
    var configTrample: Int = 0

    @ConfigInt(def = 0, min = -1, max = 1, desc = "Overrides whether vines can grow.")
    /** Override value for the `VINE_GROWTH` environment setting. */
    var configVineGrowth: Int = 0

    /** Returns the configured global override for the given environment setting. */
    fun getOverride(setting: EnvironmentSetting): Int =
        when (setting) {
            EnvironmentSetting.ANIMALS -> configAnimals
            EnvironmentSetting.MONSTERS -> configMonsters
            EnvironmentSetting.EXPLOSIONS -> configExplosions
            EnvironmentSetting.FIRE -> configFire
            EnvironmentSetting.PVP -> configPvp
            EnvironmentSetting.TRAMPLE -> configTrample
            EnvironmentSetting.VINE_GROWTH -> configVineGrowth
        }

    /** No-op lifecycle hook. */
    override fun onEnable() {}

    /** No-op lifecycle hook. */
    override fun onDisable() {}
}
