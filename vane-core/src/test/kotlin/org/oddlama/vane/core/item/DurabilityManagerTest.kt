package org.oddlama.vane.core.item

import kotlin.test.Test
import kotlin.test.assertEquals

class DurabilityManagerTest {
    @Test
    fun `keeps damage when the max does not change`() {
        assertEquals(37, DurabilityManager.scaleDamage(37, 250, 250))
    }

    @Test
    fun `scales damage proportionally to a new max`() {
        // A legacy item at 40% damage of a 500 max keeps 40% of the new 1000 max.
        assertEquals(400, DurabilityManager.scaleDamage(200, 500, 1000))
        assertEquals(125, DurabilityManager.scaleDamage(500, 1000, 250))
    }

    @Test
    fun `treats a missing old max as undamaged`() {
        assertEquals(0, DurabilityManager.scaleDamage(10, 0, 250))
    }
}
