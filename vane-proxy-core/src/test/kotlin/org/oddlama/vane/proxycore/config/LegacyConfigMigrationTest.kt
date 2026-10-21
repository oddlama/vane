package org.oddlama.vane.proxycore.config

import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LegacyConfigMigrationTest {
    private val dir = Files.createTempDirectory("vane-proxy-legacy").toFile()
    private val file = File(dir, "config.toml")

    @AfterTest
    fun cleanup() {
        dir.deleteRecursively()
    }

    @Test
    fun `migrates a pre-1_22 config and keeps a backup`() {
        val legacy =
            """
            [auth_multiplex.1]
            port = 25566
            allowed_uuids = ["b4a6717f-e3ab-4348-8e18-022827ef3177"]

            [managed_servers.my_server]
            displayName = "My Server"

            [managed_servers.my_server.online]
            motd = "Online"
            quotes = ["Ah, yes."]

            [managed_servers.my_server.offline]
            motd = "Offline"

            [managed_servers.my_server.start]
            cmd = ["/start.sh", "{SERVER}"]
            timeout = 20
            kickMsg = "Starting"
            allowAnyone = true
            """
                .trimIndent()
        file.writeText(legacy)

        val config = Config(file)

        val multiplexer = config.authMultiplex!![1]!!
        assertEquals(25566, multiplexer.port)
        assertTrue(
            multiplexer.uuidIsAllowed(
                java.util.UUID.fromString("b4a6717f-e3ab-4348-8e18-022827ef3177")
            )
        )

        val server = config.managedServers!!["my_server"]!!
        assertEquals("Online", server.motd(ManagedServer.ConfigItemSource.ONLINE))
        assertEquals("Offline", server.motd(ManagedServer.ConfigItemSource.OFFLINE))
        assertEquals(listOf("/start.sh", "my_server"), server.startCmd()!!.toList())
        assertEquals(20, server.commandTimeout())
        assertEquals("Starting", server.startKickMsg())
        assertTrue(server.start.allowAnyone)

        assertEquals(legacy, config.legacyBackup!!.readText())
        val written = file.readText()
        assertTrue("[AuthMultiplex.1]" in written, written)
        assertTrue("[ManagedServers.my_server.Start]" in written, written)
        assertTrue("KickMsg" in written && "kickMsg" !in written, written)
    }

    @Test
    fun `leaves a current config untouched`() {
        file.writeText(
            """
            [AuthMultiplex]
            [ManagedServers.lobby]
            DisplayName = "Lobby"
            """
                .trimIndent()
        )
        val config = Config(file)
        assertNull(config.legacyBackup)
        assertEquals(setOf<String?>("lobby"), config.managedServers!!.keys)
    }
}
