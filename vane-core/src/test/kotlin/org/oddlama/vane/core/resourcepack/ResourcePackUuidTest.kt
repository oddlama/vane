package org.oddlama.vane.core.resourcepack

import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ResourcePackUuidTest {
    @Test
    fun `parses configured uuids`() {
        val uuid = UUID.fromString("fbba121a-8f87-4e97-922d-2059777311bf")
        assertEquals(uuid, ResourcePackUuid.parse("fbba121a-8f87-4e97-922d-2059777311bf"))
        assertEquals(uuid, ResourcePackUuid.parse(" FBBA121A-8F87-4E97-922D-2059777311BF\n"))
    }

    @Test
    fun `rejects blank and malformed uuids`() {
        // The config default is empty, which UUID.fromString throws on.
        for (uuid in listOf(null, "", "   ", "not-a-uuid", "fbba121a8f874e97922d2059777311bf")) {
            assertNull(ResourcePackUuid.parse(uuid), "$uuid")
        }
    }

    @Test
    fun `derives a stable uuid from the url`() {
        val url = "https://example.com/pack.zip"
        assertEquals(ResourcePackUuid.fromUrl(url), ResourcePackUuid.fromUrl(url))
        assertNotEquals(
            ResourcePackUuid.fromUrl(url),
            ResourcePackUuid.fromUrl("https://example.com/other.zip"),
        )
    }
}
