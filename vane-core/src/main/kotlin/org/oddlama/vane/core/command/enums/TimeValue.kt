package org.oddlama.vane.core.command.enums

/**
 * Named world-time presets mapped to Minecraft tick values.
 *
 * @param ticks the world time in ticks.
 */
enum class TimeValue(val ticks: Int) {
    Dawn(23000),
    Day(1000),
    Noon(6000),
    Afternoon(9000),
    Dusk(13000),
    Night(14000),
    Midnight(18000);

    /** Lowercase identifier used for command parsing and completions. */
    val displayName: String
        get() = name.lowercase()
}
