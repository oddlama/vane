package org.oddlama.vane.core.command.enums

/**
 * Named weather presets mapped to storm and thunder flags.
 *
 * @param storm whether the world should be storming.
 * @param thunder whether the world should have thunder.
 */
enum class WeatherValue(val storm: Boolean, val thunder: Boolean) {
    Clear(false, false),
    Sun(false, false),
    Rain(true, false),
    Thunder(true, true);

    /** Lowercase identifier used for command parsing and completions. */
    val displayName: String
        get() = name.lowercase()
}
