package org.oddlama.vane.core.resourcepack

import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ResourcePackUrlTest {
    @Test
    fun `accepts downloadable urls`() {
        for (url in listOf(
            "https://example.com/pack.zip",
            "http://localhost:9000/vane-resource-pack.zip",
            "http://localhost:9000/vane-resource-pack.zip?3",
            "HTTPS://Example.com/a%20b.zip",
        )) {
            assertEquals(URI(url), ResourcePackUrl.parse(url), url)
        }
        assertEquals(URI("https://example.com/pack.zip"), ResourcePackUrl.parse("  https://example.com/pack.zip \n"))
    }

    @Test
    fun `rejects malformed urls that URI_create throws on`() {
        for (url in listOf("https://example.com/my pack.zip", "https://example.com/pack.zip?a=|", "http://[::1")) {
            // URI.create throws IllegalArgumentException, not URISyntaxException, which the old catch missed.
            assertFailsWith<IllegalArgumentException> { URI.create(url) }
            assertNull(ResourcePackUrl.parse(url), url)
        }
    }

    @Test
    fun `rejects urls clients cannot download from`() {
        for (url in listOf(null, "", "   ", "pack.zip", "/pack.zip", "file:///tmp/pack.zip", "ftp://example.com/pack.zip", "https:pack.zip", "https:///pack.zip")) {
            assertNull(ResourcePackUrl.parse(url), "$url")
        }
    }
}
