package org.oddlama.vane.regions.region

import org.oddlama.vane.regions.Regions

/** Region-level environment behaviors that can be configured or overridden. */
enum class EnvironmentSetting(private val def: Boolean) {
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

    /** Returns the default value when no group or global override is set. */
    fun defaultValue(): Boolean = def

    /** Returns whether a global override currently applies to this setting. */
    fun hasOverride(): Boolean = override != 0

    /** Global override value from `Regions.environmentOverrides`. */
    val override: Int
        get() = Regions.environmentOverrides?.getOverride(this) ?: 0
}
