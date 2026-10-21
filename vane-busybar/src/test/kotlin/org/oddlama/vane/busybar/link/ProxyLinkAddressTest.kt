package org.oddlama.vane.busybar.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProxyLinkAddressTest {
    private fun parse(value: String) =
        ProxyLink.parseAddress(value)?.let { "${it.hostString} ${it.port}" }

    @Test fun `defaults the port`() = assertEquals("proxy.internal 9124", parse("proxy.internal"))

    @Test fun `keeps an explicit port`() = assertEquals("10.0.0.2 9200", parse("10.0.0.2:9200"))

    @Test
    fun `accepts IPv6`() {
        assertEquals("::1 9124", parse("::1"))
        assertEquals("2001:db8::1 9200", parse("[2001:db8::1]:9200"))
    }

    @Test
    fun `rejects nonsense`() {
        assertNull(parse("https://proxy:9124"))
        assertNull(parse("proxy:99999"))
        assertNull(parse("pro xy"))
    }
}
