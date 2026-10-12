package org.oddlama.vane.busybar.link

import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.proxycore.busybar.BackendLink
import org.oddlama.vane.proxycore.busybar.BackendLinkHandler
import org.oddlama.vane.proxycore.busybar.BackendLinkServer
import org.oddlama.vane.proxycore.log.JavaCompatLogger
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Runs the backend's [ProxyLinkClient] against the proxy's [BackendLinkServer]. */
class ProxyLinkTest {
    private val secret = "correct horse battery staple"
    private val player = UUID.randomUUID()
    private val log = Logger.getLogger("ProxyLinkTest")

    /** What the proxy side observed, in order. */
    private val proxySeen = LinkedBlockingQueue<Any>()

    /** What the backend side observed, in order. */
    private val backendSeen = LinkedBlockingQueue<Any>()

    private val proxyHandler = object : BackendLinkHandler {
        override fun busyPlayers() = listOf(player)
        override fun playersWithBridges() = listOf(player)
        override fun onConnect(link: BackendLink) { proxySeen += "connect ${link.server} ${link.capabilities.sorted()}" }
        override fun onEvent(link: BackendLink, event: BackendLinkHandler.Event) { proxySeen += event }
        override fun onGrants(link: BackendLink, grants: List<BackendLink.Grants>) { proxySeen.add(grants) }
        override fun onDisconnect(link: BackendLink, reason: String) { proxySeen += "disconnect $reason" }
    }

    private val backendHandler = object : ProxyLinkClient.Handler {
        override fun onLinked(busy: Set<UUID>, bridges: Set<UUID>) { backendSeen += "linked $busy $bridges" }
        override fun onBridge(player: UUID, connected: Boolean) { backendSeen += "bridge $player $connected" }
        override fun onUnlinked() { backendSeen += "unlinked" }
        override fun onPresence(player: UUID, busy: Boolean) { backendSeen += "presence $player $busy" }
        override fun snapshot(player: UUID, reply: (JSONObject?) -> Unit) = reply(JSONObject().put("world", "world").put("ticks", 6000))
        override fun action(player: UUID, action: String, by: String, reply: (Boolean) -> Unit) {
            backendSeen += "action $action by $by"
            reply(action == "autostop.abort")
        }
    }

    private val servers = mutableListOf<BackendLinkServer>()
    private val clients = mutableListOf<ProxyLinkClient>()

    @AfterTest
    fun cleanup() {
        clients.forEach { it.stop("test over") }
        servers.forEach(BackendLinkServer::stop)
    }

    private fun freePort() = ServerSocket(0).use { it.localPort }

    private fun proxy(port: Int, known: Set<String> = setOf("survival")) =
        BackendLinkServer(JavaCompatLogger(log), secret, known::contains, proxyHandler).also {
            it.start(InetSocketAddress("127.0.0.1", port))
            servers += it
        }

    private fun backend(port: Int, server: String = "survival", secret: String = this.secret) =
        ProxyLinkClient(
            InetSocketAddress.createUnresolved("127.0.0.1", port), server, secret, "test", setOf("regions"),
            backendHandler, log
        ).also {
            it.start()
            clients += it
        }

    private fun LinkedBlockingQueue<Any>.next(): Any? = poll(5, TimeUnit.SECONDS)

    /** Links a backend and consumes the connect notices on both sides. */
    private fun linked(): Pair<BackendLinkServer, ProxyLinkClient> {
        val port = freePort()
        val proxy = proxy(port)
        val backend = backend(port)
        assertEquals("connect survival [regions]", proxySeen.next())
        assertEquals("linked [$player] [$player]", backendSeen.next())
        return proxy to backend
    }

    @Test
    fun `forwards events with their audience and the backend name`() {
        val (_, backend) = linked()
        backend.send(
            JSONObject().put("t", "event").put("type", "bedtime.enter").put("to", JSONObject().put("scope", "server"))
                .put("perm", "receive.bedtime").put("data", JSONObject().put("player", "Alex"))
        )
        backend.send(
            JSONObject().put("t", "event").put("type", "region.enter")
                .put("to", JSONObject().put("scope", "player").put("player", player.toString()))
                .put("data", JSONObject())
        )

        val bedtime = proxySeen.next() as BackendLinkHandler.Event
        assertEquals("bedtime.enter", bedtime.type)
        assertEquals(BackendLinkHandler.Scope.SERVER, bedtime.scope)
        assertEquals("receive.bedtime", bedtime.permission)
        assertEquals("Alex", bedtime.data.getString("player"))
        assertEquals("survival", bedtime.data.getString("server"))

        val region = proxySeen.next() as BackendLinkHandler.Event
        assertEquals(BackendLinkHandler.Scope.PLAYER, region.scope)
        assertEquals(player, region.player)
        assertNull(region.permission)
    }

    @Test
    fun `reports grants singly and in bulk`() {
        val (_, backend) = linked()
        val other = UUID.randomUUID()
        backend.send(
            JSONObject().put("t", "grants").put("player", player.toString()).put("granted", JSONArray(listOf("receive.players")))
                .put("online", true)
        )
        backend.send(
            JSONObject().put("t", "grants.bulk").put(
                "grants",
                JSONArray(listOf(JSONObject().put("player", other.toString()).put("granted", JSONArray())))
            )
        )
        assertEquals(listOf(BackendLink.Grants(player, listOf("receive.players"), true)), proxySeen.next())
        assertEquals(listOf(BackendLink.Grants(other, emptyList(), false)), proxySeen.next())
    }

    @Test
    fun `answers snapshot and action requests and passes presence on`() {
        val (proxy, _) = linked()
        val link = assertNotNull(proxy.link("survival"))

        val snapshot = link.requestSnapshot(player).get(5, TimeUnit.SECONDS)
        assertEquals(6000, snapshot.getInt("ticks"))

        assertTrue(link.invokeAction(player, "autostop.abort", "busybar:Alex").get(5, TimeUnit.SECONDS))
        assertEquals("action autostop.abort by busybar:Alex", backendSeen.next())
        assertFalse(link.invokeAction(player, "unknown", "x").get(5, TimeUnit.SECONDS))
        backendSeen.next()

        link.sendPresence(player, false)
        assertEquals("presence $player false", backendSeen.next())
        link.sendBridge(player, false)
        assertEquals("bridge $player false", backendSeen.next())
    }

    @Test
    fun `says goodbye when the backend stops`() {
        val (_, backend) = linked()
        backend.stop("shutdown")
        assertEquals("disconnect bye: shutdown", proxySeen.next())
    }

    @Test
    fun `reconnects after the proxy restarts`() {
        val port = freePort()
        val proxy = proxy(port)
        backend(port)
        assertEquals("connect survival [regions]", proxySeen.next())
        assertEquals("linked [$player] [$player]", backendSeen.next())

        proxy.stop()
        servers.remove(proxy)
        assertEquals("unlinked", backendSeen.next())
        assertEquals("disconnect proxy stopping", proxySeen.next())

        proxy(port)
        assertEquals("connect survival [regions]", proxySeen.next())
        assertEquals("linked [$player] [$player]", backendSeen.next())
    }

    @Test
    fun `rejects a wrong secret`() {
        val port = freePort()
        proxy(port)
        val backend = backend(port, secret = "a different secret entirely")
        assertNull(proxySeen.poll(1, TimeUnit.SECONDS))
        assertFalse(backend.linked)
    }

    @Test
    fun `rejects a server the proxy does not know`() {
        val port = freePort()
        proxy(port)
        val backend = backend(port, server = "creative")
        assertNull(proxySeen.poll(1, TimeUnit.SECONDS))
        assertFalse(backend.linked)
    }

    @Test
    fun `a second backend under the same name replaces the first`() {
        val port = freePort()
        val proxy = proxy(port)
        val first = backend(port)
        assertEquals("connect survival [regions]", proxySeen.next())
        assertEquals("linked [$player] [$player]", backendSeen.next())

        val second = backend(port)
        // The old link's reader reports its end on its own thread, so the order varies.
        assertEquals(setOf("connect survival [regions]", "disconnect replaced"), setOf(proxySeen.next(), proxySeen.next()))
        assertEquals(setOf("linked [$player] [$player]", "unlinked"), setOf(backendSeen.next(), backendSeen.next()))
        assertTrue(second.linked)
        assertNotNull(proxy.link("survival"))
        // Otherwise the first one reconnects and replaces the second in turn.
        first.stop("test over")
    }
}
