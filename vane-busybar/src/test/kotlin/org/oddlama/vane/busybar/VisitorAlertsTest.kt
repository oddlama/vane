package org.oddlama.vane.busybar

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class VisitorAlertsTest {
    private val owner = UUID.randomUUID()
    private val base = UUID.randomUUID()
    private val farm = UUID.randomUUID()
    private val players = generateSequence { UUID.randomUUID() }.take(10).toList()
    private val sent = mutableListOf<VisitorAlerts.Alert>()
    private val alerts =
        VisitorAlerts(windowMillis = 60_000, cooldownMillis = 600_000) { sent += it }

    private fun visit(seconds: Int, player: Int, region: UUID = base) =
        alerts.visit(
            seconds * 1000L,
            region,
            if (region == base) "Base" else "Farm",
            owner,
            players[player],
            "P$player",
            "skin$player",
        )

    @Test
    fun `reports the first visitor right away`() {
        visit(0, 0)
        assertEquals(
            listOf(VisitorAlerts.Alert(owner, base, "Base", listOf("P0"), "skin0", 1)),
            sent,
        )
    }

    @Test
    fun `collects later visitors into one summary per window`() {
        visit(0, 0)
        visit(2, 1)
        visit(3, 2)
        visit(40, 3)
        assertEquals(1, sent.size)
        alerts.flush(60_000)
        assertEquals(
            VisitorAlerts.Alert(owner, base, "Base", listOf("P1", "P2", "P3"), "skin1", 3),
            sent[1],
        )
    }

    @Test
    fun `names at most five visitors but counts all`() {
        visit(0, 0)
        (1..8).forEach { visit(it, it) }
        alerts.flush(60_000)
        assertEquals(listOf("P1", "P2", "P3", "P4", "P5"), sent[1].visitors)
        assertEquals(8, sent[1].count)
    }

    @Test
    fun `keeps summarizing a steady stream and reports at once after a quiet window`() {
        visit(0, 0)
        visit(30, 1)
        alerts.flush(60_000) // summary of P1, next window opens
        visit(90, 2)
        alerts.flush(120_000) // summary of P2
        alerts.flush(180_000) // quiet window closes without a report
        visit(200, 3) // region is quiet again: immediate
        assertEquals(listOf(1, 1, 1, 1), sent.map { it.count })
        assertEquals(listOf("P0", "P1", "P2", "P3"), sent.map { it.visitors.single() })
    }

    @Test
    fun `counts a pacing visitor once per cooldown`() {
        visit(0, 0)
        visit(5, 0)
        visit(70, 0)
        alerts.flush(130_000)
        assertEquals(1, sent.size)
        visit(601, 0)
        assertEquals(2, sent.size)
    }

    @Test
    fun `keeps regions apart`() {
        visit(0, 0, base)
        visit(1, 1, farm)
        assertEquals(listOf("Base", "Farm"), sent.map { it.regionName })
    }
}
