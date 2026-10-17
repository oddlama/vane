package org.oddlama.vane.core.menu

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent.Reason
import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.functional.Consumer2
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MenuCloseTest {
    private val player = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(Player::class.java)) { _, _, _ -> null } as Player

    /** Closes with [reason] and returns which callbacks ran, as "natural" and "close(<reason>)". */
    private fun close(reason: Reason, natural: Boolean, generic: Boolean): List<String> {
        val calls = mutableListOf<String>()
        Menu.dispatchClose(
            player,
            reason,
            if (natural) Consumer1 { p -> assertSame(player, p); calls += "natural" } else null,
            if (generic) Consumer2 { p, r -> assertSame(player, p); calls += "close($r)" } else null,
        )
        return calls
    }

    @Test
    fun `player close prefers the natural-close callback`() {
        assertEquals(listOf("natural"), close(Reason.PLAYER, natural = true, generic = true))
        assertEquals(listOf("natural"), close(Reason.PLAYER, natural = true, generic = false))
    }

    @Test
    fun `player close falls back to the generic callback`() {
        assertEquals(listOf("close(PLAYER)"), close(Reason.PLAYER, natural = false, generic = true))
    }

    @Test
    fun `other reasons only run the generic callback`() {
        for (reason in Reason.entries.filter { it != Reason.PLAYER }) {
            assertEquals(listOf("close($reason)"), close(reason, natural = true, generic = true), "$reason")
            assertEquals(listOf("close($reason)"), close(reason, natural = false, generic = true), "$reason")
            assertEquals(listOf(), close(reason, natural = true, generic = false), "$reason")
        }
    }

    @Test
    fun `no callbacks is a no-op for every reason`() {
        for (reason in Reason.entries) assertEquals(listOf(), close(reason, natural = false, generic = false), "$reason")
    }
}
