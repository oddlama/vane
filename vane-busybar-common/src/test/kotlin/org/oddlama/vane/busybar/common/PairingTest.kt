package org.oddlama.vane.busybar.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PairingTest {
    private fun resolve(configured: String?, serverIp: String = "", scheme: String = "https", direct: Boolean = true) =
        Pairing.resolvePublicUrl(configured, serverIp, 9123, scheme, direct)

    @Test
    @Suppress("HttpUrlsUsage") // plain HTTP is under test
    fun `adds scheme and port to a bare host`() {
        assertEquals("https://203.0.113.7:9123", resolve(" 203.0.113.7/ "))
        assertEquals("http://mc.example.com:9123", resolve("mc.example.com", scheme = "http"))
    }

    @Test
    fun `keeps a port or path given with a bare host`() {
        assertEquals("https://mc.example.com:443", resolve("mc.example.com:443"))
        assertEquals("https://mc.example.com:9123/busybar", resolve("mc.example.com/busybar"))
        assertEquals("https://[2001:db8::1]:8443", resolve("[2001:db8::1]:8443"))
        assertEquals("https://[2001:db8::1]:9123", resolve("2001:db8::1"))
    }

    @Test
    fun `adds the port to a full URL without one when bridges connect directly`() {
        assertEquals("https://localhost:9123", resolve("https://localhost"))
        assertEquals("https://mc.example.com:9123/busybar", resolve("https://mc.example.com/busybar/"))
        assertEquals("https://[2001:db8::1]:9123", resolve("https://[2001:db8::1]"))
        assertEquals("https://mc.example.com:443", resolve("https://mc.example.com:443"))
    }

    @Test
    fun `uses a full URL as is behind a reverse proxy`() {
        assertEquals("https://mc.example.com/busybar", resolve("https://mc.example.com/busybar/", direct = false))
        assertEquals("http://10.0.0.5:9123", resolve("http://10.0.0.5:9123", direct = false))
    }

    @Test
    fun `falls back to server-ip`() {
        assertEquals("https://192.168.1.5:9123", resolve("", serverIp = "192.168.1.5"))
        assertEquals("https://[2001:db8::2]:9123", resolve(null, serverIp = "2001:db8::2"))
    }

    @Test
    fun `is unknown without an address`() {
        assertNull(resolve(""))
        assertNull(resolve(null, serverIp = "0.0.0.0"))
    }

    @Test
    fun `rejects values a pairing string cannot carry`() {
        assertNull(resolve("ftp://mc.example.com"))
        assertNull(resolve("https://user@mc.example.com"))
        assertNull(resolve("https://mc.example.com#x"))
        assertNull(resolve("https://mc.example.com?a=b"))
        assertNull(resolve("not a host"))
        assertNull(resolve("https://"))
    }

    @Test
    fun `leaves https implied in pairing strings`() {
        assertEquals("tok@localhost:9123#sha256=PIN", Pairing.pairingString("tok", "https://localhost:9123", "PIN"))
        assertEquals("tok@mc.example.com/busybar", Pairing.pairingString("tok", "https://mc.example.com/busybar", null))
    }

    @Test
    fun `keeps plain http explicit in pairing strings`() {
        assertEquals("http://tok@10.0.0.5:9123", Pairing.pairingString("tok", "http://10.0.0.5:9123", null))
    }
}
