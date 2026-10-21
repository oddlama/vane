package org.oddlama.vane.proxycore.util

/** Maps supported time suffixes to their multiplier in milliseconds. */
private val timeMultiplier: Map<Char, Long> =
    mapOf(
        's' to 1_000L,
        'm' to 60_000L,
        'h' to 3_600_000L,
        'd' to 86_400_000L,
        'w' to 604_800_000L,
        'y' to 31_536_000_000L,
    )

/** Boundary between a unit and the digits of the next segment, e.g. `1h|30m`. */
private val SEGMENT_BOUNDARY = Regex("(?<=[^0-9])(?=[0-9])")

/** Boundary between a segment's digits and its unit, e.g. `30|m`. */
private val NUMBER_UNIT_BOUNDARY = Regex("(?=[^0-9])")

/** Separators and filler punctuation allowed inside a unit, e.g. `2d, and 6h`. */
private val UNIT_NOISE = Regex("[,+.\\s]+")

/**
 * Parses a compact duration string into milliseconds.
 *
 * Examples: `10s`, `1h30m`, `2d and 6h`.
 *
 * @param input the input duration string.
 * @return the parsed duration in milliseconds.
 * @throws NumberFormatException if the input cannot be parsed.
 */
@Throws(NumberFormatException::class)
fun parseTime(input: String): Long {
    /** Accumulated parsed duration in milliseconds. */
    var ret = 0L

    /** Parsed alternating numeric/unit segments. */
    val parts = input.split(SEGMENT_BOUNDARY).filter { it.isNotEmpty() }
    for (time in parts) {
        val content = time.split(NUMBER_UNIT_BOUNDARY).filter { it.isNotEmpty() }
        if (content.size != 2) throw NumberFormatException("missing multiplier")
        val numberPart = content[0]
        val unitPart = content[1].replace("and", "").replace(UNIT_NOISE, "")
        if (unitPart.isEmpty()) throw NumberFormatException("missing multiplier")
        val mult =
            timeMultiplier[unitPart[0]]
                ?: throw NumberFormatException("unknown multiplier: ${unitPart[0]}")
        ret += numberPart.toLong() * mult
    }
    return ret
}

/**
 * Formats a duration in milliseconds as a compact duration string.
 *
 * @param millis the duration in milliseconds.
 * @return the formatted duration, such as `1h30m`.
 */
fun formatTime(millis: Long): String {
    /** Whole days component. */
    val days = millis / 86_400_000L

    /** Remaining hours component after days. */
    val hours = (millis / 3_600_000L) % 24

    /** Remaining minutes component after hours. */
    val minutes = (millis / 60_000L) % 60

    /** Remaining seconds component after minutes. */
    val seconds = (millis / 1_000L) % 60
    return buildString {
        if (days > 0) append("${days}d")
        if (hours > 0) append("${hours}h")
        if (minutes > 0) append("${minutes}m")
        if (seconds > 0 || isEmpty()) append("${seconds}s")
    }
}
