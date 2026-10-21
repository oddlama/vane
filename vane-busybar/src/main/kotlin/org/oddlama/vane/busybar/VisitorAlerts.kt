package org.oddlama.vane.busybar

import java.util.*

/**
 * Decides when a region owner hears about visitors, so a busy region rings the Bar once instead of
 * for every player walking in.
 *
 * - The first visitor of a quiet region is reported right away.
 * - Visitors after that are collected for [windowMillis] and reported together, at most [MAX_NAMES]
 *   by name, with the total in [Alert.count]. A window that collected anyone is followed by another
 *   one, so a steady stream of visitors yields one summary per window.
 * - Each visitor counts at most once per region per [cooldownMillis], so someone pacing along a
 *   border is reported once.
 *
 * Holds no Bukkit state: the caller passes the time and delivers what [emit] receives.
 *
 * @param windowMillis how long visitors are collected after an alert.
 * @param cooldownMillis how long a visitor stays unreported in a region after being counted.
 * @param emit receives each alert.
 */
class VisitorAlerts(
    private val windowMillis: Long = 60_000L,
    private val cooldownMillis: Long = 10 * 60_000L,
    private val emit: (Alert) -> Unit,
) {
    /** One report for [owner]: [count] visitors of [regionName], the first [visitors] by name. */
    data class Alert(
        val owner: UUID,
        val region: UUID,
        val regionName: String,
        val visitors: List<String>,
        val head: String?,
        val count: Int,
    )

    /** Visitors collected for one region until [endsAt]. */
    private class Window(val owner: UUID, var regionName: String, val endsAt: Long) {
        val names = mutableListOf<String>()
        var head: String? = null
        var count = 0
    }

    /** Open windows by region. */
    private val windows = mutableMapOf<UUID, Window>()

    /** When each visitor was last counted in each region. */
    private val counted = mutableMapOf<Pair<UUID, UUID>, Long>()

    /**
     * Records that [visitor] entered [region], owned by [owner].
     *
     * @param head skin id of the visitor, for the face on the Bar.
     */
    fun visit(
        now: Long,
        region: UUID,
        regionName: String,
        owner: UUID,
        visitor: UUID,
        visitorName: String,
        head: String?,
    ) {
        flush(now)
        val key = visitor to region
        counted[key]?.let { if (now - it < cooldownMillis) return }
        counted[key] = now

        val window = windows[region]
        if (window == null) {
            emit(Alert(owner, region, regionName, listOf(visitorName), head, 1))
            windows[region] = Window(owner, regionName, now + windowMillis)
            return
        }
        window.regionName = regionName
        window.count++
        if (window.names.size < MAX_NAMES) window.names += visitorName
        if (window.head == null) window.head = head
    }

    /** Reports and closes the windows that ended by [now], opening a new one after each report. */
    fun flush(now: Long) {
        val ended = windows.filterValues { now >= it.endsAt }
        for ((region, window) in ended) {
            windows.remove(region)
            if (window.count == 0) continue
            emit(
                Alert(
                    window.owner,
                    region,
                    window.regionName,
                    window.names.toList(),
                    window.head,
                    window.count,
                )
            )
            windows[region] = Window(window.owner, window.regionName, now + windowMillis)
        }
        counted.values.removeIf { now - it >= cooldownMillis }
    }

    /** Forgets everything, for example when the integration stops. */
    fun clear() {
        windows.clear()
        counted.clear()
    }

    companion object {
        /** Visitors named in one alert; the rest only add to its count. */
        const val MAX_NAMES = 5
    }
}
