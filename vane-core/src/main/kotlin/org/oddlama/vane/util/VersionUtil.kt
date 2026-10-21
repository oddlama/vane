package org.oddlama.vane.util

/** Semantic version comparison for release tags such as `v1.22.0` or `v1.23.0-beta.1`. */
object VersionUtil {
    private val SEMVER = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+.*)?$""")

    /**
     * Compares two versions by semver precedence: a pre-release sorts before its release
     * (`1.23.0-beta.1 < 1.23.0`), and pre-release identifiers compare numerically when both are
     * numbers, lexically otherwise.
     *
     * @return negative, zero or positive like [java.util.Comparator.compare], or `null` if either
     *   is unparsable.
     */
    @JvmStatic
    fun compare(a: String, b: String): Int? {
        val ma = SEMVER.matchEntire(a.trim()) ?: return null
        val mb = SEMVER.matchEntire(b.trim()) ?: return null
        for (i in 1..3) {
            val diff = ma.groupValues[i].toBigInteger().compareTo(mb.groupValues[i].toBigInteger())
            if (diff != 0) return diff
        }
        return comparePreRelease(ma.groupValues[4], mb.groupValues[4])
    }

    /**
     * Returns true if [candidate] is a strictly newer version than [current]; false if either is
     * unparsable.
     */
    @JvmStatic
    fun isNewer(candidate: String, current: String): Boolean =
        (compare(candidate, current) ?: 0) > 0

    private fun comparePreRelease(a: String, b: String): Int {
        if (a == b) return 0
        // A release (no pre-release part) has higher precedence than any of its pre-releases.
        if (a.isEmpty()) return 1
        if (b.isEmpty()) return -1
        val pa = a.split('.')
        val pb = b.split('.')
        for (i in 0 until minOf(pa.size, pb.size)) {
            val na = pa[i].toBigIntegerOrNull()
            val nb = pb[i].toBigIntegerOrNull()
            val diff =
                when {
                    na != null && nb != null -> na.compareTo(nb)
                    na != null -> -1 // Numeric identifiers sort before alphanumeric ones.
                    nb != null -> 1
                    else -> pa[i].compareTo(pb[i])
                }
            if (diff != 0) return diff
        }
        return pa.size.compareTo(pb.size)
    }
}
