package org.oddlama.vane.util

/** Converts milliseconds to server ticks. */
fun msToTicks(ms: Long): Long = ms / 50

/** Converts server ticks to milliseconds. */
fun ticksToMs(ticks: Long): Long = ticks * 50

/** Returns cumulative experience required for the given level. */
fun expForLevel(level: Int): Int =
    when {
        level < 17 -> level * level + 6 * level
        level < 32 -> (2.5 * level * level - 40.5 * level).toInt() + 360
        else -> (4.5 * level * level - 162.5 * level).toInt() + 2220
    }
