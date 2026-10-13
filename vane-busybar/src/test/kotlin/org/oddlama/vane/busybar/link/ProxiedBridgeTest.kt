package org.oddlama.vane.busybar.link

import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.proxycore.ProxyPlayer
import org.oddlama.vane.proxycore.ProxyServer
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.log.JavaCompatLogger
import org.oddlama.vane.proxycore.scheduler.ProxyTaskScheduler
import java.awt.image.BufferedImage
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Runs a bridge against the proxy's [org.oddlama.vane.proxycore.busybar.BusyBarProxy], fed by a
 * backend through a real [ProxyLinkClient]: the whole path of a proxied BUSY Bar, without Velocity
 * or Paper.
 */
class ProxiedBridgeTest {
    private val secret = "correct horse battery staple"
    private val alex = UUID.randomUUID()
    private val blake = UUID.randomUUID()
    private val log = Logger.getLogger("ProxiedBridgeTest")
    private val folder: File = Files.createTempDirectory("busybar-proxy").toFile()
    private val bridgePort = freePort()
    private val linkPort = freePort()

    /** Backend each online player is on. */
    private val online = ConcurrentHashMap<UUID, String>()

    private class Player(override val uniqueId: UUID, override val name: String) : ProxyPlayer {
        override fun disconnect(message: String?) {}
        override val ping = 0L
        override fun sendMessage(message: String?) {}
    }

    private val names = mapOf(alex to "Alex", blake to "Blake")

    private val proxyServer = object : ProxyServer {
        override val scheduler: ProxyTaskScheduler? = null
        override fun broadcast(message: String?) {}
        override val players: MutableCollection<ProxyPlayer?>
            get() = online.keys.mapTo(mutableListOf()) { Player(it, names.getValue(it)) }
        override fun hasPermission(uuid: UUID?, vararg permission: String?) = false
        override fun hasServer(name: String) = name in setOf("survival", "creative")
        override fun serverOf(player: UUID) = online[player]
        override val maxPlayers = 20
        override val favicon: String? = null
        override val onlineMode = true
    }

    private val plugin = object : VaneProxyPlugin() {}

    /** What the backend was asked to do, in order. */
    private val backendSeen = LinkedBlockingQueue<String>()

    private val backendHandler = object : ProxyLinkClient.Handler {
        override fun onLinked(busy: Set<UUID>, bridges: Set<UUID>) { backendSeen += "linked $bridges" }
        override fun onUnlinked() {}
        override fun onBridge(player: UUID, connected: Boolean) { backendSeen += "bridge ${names[player]} $connected" }
        override fun onPresence(player: UUID, busy: Boolean) { backendSeen += "presence ${names[player]} $busy" }
        override fun snapshot(player: UUID, reply: (JSONObject?) -> Unit) =
            reply(JSONObject().put("world", "world").put("ticks", 6000).put("tps", 20.0).put("players", 99))
        override fun action(player: UUID, action: String, by: String, reply: (Boolean) -> Unit) {
            backendSeen += "action $action by $by"
            reply(true)
        }
    }

    private lateinit var backend: ProxyLinkClient
    private val http = HttpClient.newHttpClient()
    private val streams = mutableListOf<Thread>()

    /** Server list icon of the survival backend. */
    private val survivalIcon = File(folder, "survival.png").also {
        ImageIO.write(BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", it)
    }

    @BeforeTest
    fun start() {
        File(folder, "config.toml").writeText(
            """
            [AuthMultiplex]
            [ManagedServers.survival.Online]
            Favicon = "${survivalIcon.absolutePath}"
            [BusyBar]
            Enabled = true
            Bind = "127.0.0.1"
            Port = $bridgePort
            PublicUrl = "127.0.0.1"
            Tls = "off"
                [BusyBar.Link]
                Port = $linkPort
                Secret = "$secret"
            """.trimIndent()
        )
        plugin.logger = JavaCompatLogger(log)
        plugin.server = proxyServer
        plugin.dataFolder = folder
        assertTrue(plugin.config.load())
        plugin.busyBar.start()
        assertTrue(plugin.busyBar.running)

        backend = ProxyLinkClient(
            InetSocketAddress.createUnresolved("127.0.0.1", linkPort), "survival", secret, "test", emptySet(),
            backendHandler, log
        )
        backend.start()
        assertEquals("linked []", backendSeen.next())
    }

    @AfterTest
    fun stop() {
        backend.stop("test over")
        plugin.busyBar.stop()
        streams.forEach(Thread::interrupt)
        folder.deleteRecursively()
    }

    private fun freePort() = ServerSocket(0).use { it.localPort }

    private fun <T> LinkedBlockingQueue<T & Any>.next(): T? = poll(5, TimeUnit.SECONDS)

    /** Joins [player] to the network on [server]. */
    private fun join(player: UUID, server: String) {
        online[player] = server
        plugin.busyBar.onPlayerJoin(player, names.getValue(player), null)
        plugin.busyBar.onServerSwitch(player, server)
    }

    /** Links [player] and returns the token from the pairing string. */
    @Suppress("HttpUrlsUsage") // The proxy runs with Tls off, so pairing strings are plain HTTP.
    private fun token(player: UUID): String {
        val pairing = assertNotNull(plugin.busyBar.link(player))
        assertTrue(pairing.startsWith("http://"), pairing)
        return pairing.removePrefix("http://").substringBefore('@')
    }

    /** Opens a bridge stream for [token]; events arrive as `type data` lines. */
    private fun bridge(token: String): LinkedBlockingQueue<Pair<String, JSONObject>> {
        val events = LinkedBlockingQueue<Pair<String, JSONObject>>()
        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$bridgePort/events"))
            .header("Authorization", "Bearer $token").build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofLines())
        assertEquals(200, response.statusCode())
        streams += Thread.ofVirtual().start {
            var type = ""
            for (line in response.body()) {
                when {
                    line.startsWith("event: ") -> type = line.removePrefix("event: ")
                    line.startsWith("data: ") -> events += type to JSONObject(line.removePrefix("data: "))
                }
            }
        }
        return events
    }

    private fun post(token: String, body: JSONObject): Int {
        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$bridgePort/action"))
            .header("Authorization", "Bearer $token")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build()
        return http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
    }

    private fun backendEvent(type: String, to: JSONObject, perm: String?, data: JSONObject = JSONObject()) =
        backend.send(
            JSONObject().put("t", "event").put("type", type).put("to", to).put("data", data).also { f -> perm?.let { f.put("perm", it) } }
        )

    @Test
    fun `the snapshot merges the backend's world with the network's players`() {
        join(alex, "survival")
        val events = bridge(token(alex))
        val (type, snapshot) = assertNotNull(events.next())
        assertEquals("snapshot", type)
        assertEquals("survival", snapshot.getString("server"))
        assertEquals(6000, snapshot.getInt("ticks"))
        assertEquals(1, snapshot.getInt("players"), "the network count wins over the backend's")
        assertFalse(snapshot.has("tps"), "receive.server defaults to operators only")
        assertEquals(listOf("receive.players", "receive.bedtime"), snapshot.getJSONArray("permissions").toList().take(2))
        assertEquals("bridge Alex true", backendSeen.next())
    }

    @Test
    fun `events reach the bridges they are meant for`() {
        join(alex, "survival")
        join(blake, "creative")
        val alexEvents = bridge(token(alex)).also { it.next() }
        val blakeEvents = bridge(token(blake)).also { it.next() }

        backendEvent("bedtime.enter", JSONObject().put("scope", "server"), "receive.bedtime", JSONObject().put("player", "Alex"))
        backendEvent("server.tps", JSONObject().put("scope", "server"), "receive.server")
        backendEvent("portal.activate", JSONObject().put("scope", "player").put("player", blake.toString()), "receive.portals")

        val (type, bedtime) = assertNotNull(alexEvents.next())
        assertEquals("bedtime.enter", type)
        assertEquals("survival", bedtime.getString("server"))
        assertEquals("portal.activate", blakeEvents.next()?.first, "Blake is on creative, so no bedtime")
        assertNull(alexEvents.poll(500, TimeUnit.MILLISECONDS), "TPS needs receive.server")

        // Once the backend reports Alex may see it, TPS gets through.
        backend.send(
            JSONObject().put("t", "grants").put("player", alex.toString())
                .put("granted", JSONArray(listOf("receive.server", "receive.bedtime")))
        )
        assertEquals("permissions.updated", alexEvents.next()?.first)
        backendEvent("server.tps", JSONObject().put("scope", "server"), "receive.server")
        assertEquals("server.tps", alexEvents.next()?.first)
    }

    @Test
    fun `players joining the network are announced by the proxy`() {
        join(alex, "survival")
        val events = bridge(token(alex)).also { it.next() }
        join(blake, "creative")
        val (type, data) = assertNotNull(events.next())
        assertEquals("player.join", type)
        assertEquals("Blake", data.getString("player"))
        assertEquals(2, data.getInt("online"))
    }

    @Test
    fun `focus mode set by the bridge reaches every backend`() {
        join(alex, "survival")
        val token = token(alex)
        val events = bridge(token).also { it.next() }
        backendSeen.next()

        assertEquals(202, post(token, JSONObject().put("type", "presence.set").put("busy", true)))
        assertEquals("presence.updated", events.next()?.first)
        assertEquals("presence Alex true", backendSeen.next())
        assertTrue(plugin.busyBar.isBusy(alex))
    }

    @Test
    fun `autostop is aborted on the backend that is stopping`() {
        join(alex, "survival")
        backend.send(
            JSONObject().put("t", "grants").put("player", alex.toString())
                .put("granted", JSONArray(listOf("receive.autostop", "action.autostop_abort")))
        )
        val token = token(alex)
        val abort = JSONObject().put("type", "action.invoke").put("action", "autostop.abort")
        assertEquals(409, post(token, abort), "nothing is stopping yet")

        backendEvent("autostop.scheduled", JSONObject().put("scope", "all"), "receive.autostop", JSONObject().put("remaining_s", 60))
        // The event is handled on the link's thread; wait for it to arrive.
        val deadline = System.currentTimeMillis() + 5_000
        var status = post(token, abort)
        while (status == 409 && System.currentTimeMillis() < deadline) status = post(token, abort)
        assertEquals(202, status)
        assertEquals("action autostop.abort by busybar:Alex", backendSeen.next())
    }

    @Test
    fun `a backend shutting down is reported to its players`() {
        join(alex, "survival")
        val events = bridge(token(alex)).also { it.next() }
        backend.stop("shutdown")
        val (type, data) = assertNotNull(events.next())
        assertEquals("server.stop", type)
        assertEquals("survival", data.getString("server"))
    }

    @Test
    fun `bridges get the icon of the server their owner is on`() {
        join(alex, "survival")
        join(blake, "creative")
        val alexToken = token(alex)
        val alexIcon = assertNotNull(bridge(alexToken).next()).second.getString("icon")
        assertTrue(assertNotNull(bridge(token(blake)).next()).second.isNull("icon"), "creative has no icon")

        val response = http.send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:$bridgePort/icon")).header("Authorization", "Bearer $alexToken").build(),
            HttpResponse.BodyHandlers.ofByteArray()
        )
        assertEquals(200, response.statusCode())
        assertEquals(alexIcon, response.headers().firstValue("ETag").orElse(null))
        assertTrue(response.body().contentEquals(survivalIcon.readBytes()))
    }

    @Test
    fun `other backends cannot overwrite the grants of a player who is not on them`() {
        join(alex, "survival")
        val events = bridge(token(alex)).also { it.next() }
        assertEquals("bridge Alex true", backendSeen.next())
        val creative = ProxyLinkClient(
            InetSocketAddress.createUnresolved("127.0.0.1", linkPort), "creative", secret, "test", emptySet(),
            backendHandler, log
        )
        creative.start()
        try {
            assertEquals("linked [$alex]", backendSeen.next())
            fun grants(online: Boolean, vararg granted: String) = JSONObject().put("t", "grants").put("player", alex.toString())
                .put("granted", JSONArray(granted.toList())).put("online", online)

            // Creative refreshing Alex while offline there says nothing about survival's permissions.
            creative.send(grants(false, "receive.server"))
            assertNull(events.poll(500, TimeUnit.MILLISECONDS))
            assertFalse("receive.server" in grantedNow())

            // Once Alex is on creative, its report counts.
            online[alex] = "creative"
            creative.send(grants(true, "receive.server"))
            assertEquals("permissions.updated", events.next()?.first)
            assertEquals(listOf("receive.server"), grantedNow())
        } finally {
            creative.stop("test over")
        }
    }

    /** Alex's permissions according to a fresh snapshot. */
    private fun grantedNow(): List<Any?> = assertNotNull(plugin.busyBar.snapshot(alex)).getJSONArray("permissions").toList()

    @Test
    fun `unknown and revoked tokens are refused`() {
        join(alex, "survival")
        val token = token(alex)
        assertTrue(plugin.busyBar.unlink(alex))
        assertEquals(401, post(token, JSONObject().put("type", "presence.set").put("busy", true)))
        assertEquals(401, post("not-a-token", JSONObject()))
    }
}
