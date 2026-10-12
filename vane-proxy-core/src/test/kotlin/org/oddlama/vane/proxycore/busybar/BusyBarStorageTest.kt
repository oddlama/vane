package org.oddlama.vane.proxycore.busybar

import java.nio.file.Files
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BusyBarStorageTest {
    private val file = Files.createTempDirectory("busybar-storage").resolve("busybar.json").toFile()
    private val alex = UUID.randomUUID()

    @Test
    fun `survives a reload`() {
        BusyBarStorage(file).apply {
            setTokenHash(alex, "hash")
            assertTrue(setBusy(alex, true))
            assertTrue(setGrants(alex, listOf("receive.players")))
            setName(alex, "Alex")
            setLastServer(alex, "survival")
        }
        BusyBarStorage(file).apply {
            load()
            assertEquals(mapOf(alex to "hash"), tokenHashes())
            assertEquals(setOf(alex), busyPlayers())
            assertEquals(listOf("receive.players"), grants(alex))
            assertEquals(alex, playerNamed("alex"))
            assertEquals("survival", lastServer(alex))
        }
    }

    @Test
    fun `reports only real changes`() {
        val storage = BusyBarStorage(file)
        assertTrue(storage.setGrants(alex, listOf("a")))
        assertFalse(storage.setGrants(alex, listOf("a")))
        assertFalse(storage.setBusy(alex, false))
        storage.setTokenHash(alex, null)
        assertNull(storage.tokenHashes()[alex])
    }
}
