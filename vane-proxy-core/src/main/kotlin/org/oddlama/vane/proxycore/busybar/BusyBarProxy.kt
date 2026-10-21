package org.oddlama.vane.proxycore.busybar

import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.security.GeneralSecurityException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.busybar.common.BridgeLog
import org.oddlama.vane.busybar.common.BridgePermissions
import org.oddlama.vane.busybar.common.BridgeServer
import org.oddlama.vane.busybar.common.BridgeTls
import org.oddlama.vane.busybar.common.Heads
import org.oddlama.vane.busybar.common.LinkProtocol
import org.oddlama.vane.busybar.common.Pairing
import org.oddlama.vane.proxycore.VaneProxyPlugin
import org.oddlama.vane.proxycore.config.BusyBarConfig
import org.oddlama.vane.proxycore.config.ManagedServer

/**
 * Serves BUSY Bar bridges for the whole network when `[BusyBar]` is enabled.
 *
 * Players link a bridge with `/busybar link` on the proxy. The bridge connects to the proxy's
 * [BridgeServer], so it keeps working while its owner moves between servers. The proxy reports
 * players joining and leaving the network itself; everything about worlds, regions, or autostop
 * comes from the vane-busybar backends through [BackendLinkServer], and is delivered to the bridges
 * whose owners should see it, judged by the grants backends report.
 *
 * Focus mode and tokens live here; backends mirror focus mode for their tab list tag.
 *
 * @param plugin owning plugin.
 */
class BusyBarProxy(private val plugin: VaneProxyPlugin) : BackendLinkHandler, BridgeServer.Host {
    private val log = BridgeLog { level, message, error ->
        plugin.getLogger().log(level, message, error)
    }

    /** Settings the feature was started with, while running. */
    @Volatile private var config: BusyBarConfig? = null

    /** The running link server, if backends can report. */
    var links: BackendLinkServer? = null
        private set

    /** The server bridges connect to, while running. */
    private var bridges: BridgeServer? = null

    private var tls: BridgeTls? = null
    private var storage: BusyBarStorage? = null
    private val heads = Heads()

    /** Token hash to player, read from HTTP threads. */
    private val tokenIndex = ConcurrentHashMap<String, UUID>()

    /** Skin id of each online player, for their quit event. */
    private val onlineHeads = ConcurrentHashMap<UUID, String>()

    /** Backends with an autostop pending, which `autostop.abort` may target. */
    private val pendingAutostops: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** Players with at least one bridge connected. */
    private val withBridge: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /** Whether the proxy serves BUSY Bars. */
    val running: Boolean
        get() = bridges != null

    /** Starts serving when the config enables it; logs and stays off on errors. */
    fun start() {
        val config = plugin.config.busyBar?.takeIf { it.enabled } ?: return
        val folder = checkNotNull(plugin.dataFolder) { "Data folder is not initialized" }
        val storage = BusyBarStorage(File(folder, "busybar.json"))
        try {
            storage.load()
        } catch (e: IOException) {
            plugin
                .getLogger()
                .log(
                    Level.SEVERE,
                    "Could not read BUSY Bar storage, so BUSY Bar support stays off",
                    e,
                )
            return
        }
        this.config = config
        this.storage = storage
        tokenIndex.clear()
        storage.tokenHashes().forEach { (player, hash) -> tokenIndex[hash] = player }
        tls = BridgeTls(folder, log)

        if (BridgeTls.parseMode(config.tls) == null) {
            log.warning(
                "Unknown BusyBar.Tls mode '${config.tls}', using auto. Valid modes: auto, keystore, off."
            )
        }
        if (!plugin.proxy.onlineMode) {
            log.warning(
                "The proxy runs in offline mode, so Minecraft traffic is unencrypted. Pairing strings from " +
                    "/busybar link, which hold the token and the key fingerprint, can be read or altered on the way."
            )
        }

        val server = BridgeServer(this, heads, log)
        server.setIcon(plugin.proxy.favicon?.let(::decodeDataUrl))
        // Bridges show the icon of the server their owner is on, as the server list does.
        plugin.config.managedServers.forEach { (name, managed) ->
            val icon = managed?.favicon(ManagedServer.ConfigItemSource.ONLINE) ?: return@forEach
            server.setIcon(name ?: return@forEach, decodeDataUrl(icon))
        }
        bridges = server
        if (!startBridges()) {
            bridges = null
            this.config = null
            this.storage = null
            return
        }
        if (publicUrl() == null) {
            log.warning(
                "BusyBar.PublicUrl is ${if (config.publicUrl.isBlank()) "empty" else "invalid"}, so /busybar link " +
                    "cannot tell players where to connect. Set it to the address bridges reach this proxy at, " +
                    "for example mc.example.com or 203.0.113.7."
            )
        }
        startLinks(config)
    }

    /** Tells bridges the network is going away, then closes every bridge and backend link. */
    fun stop() {
        bridges?.emit(
            "server.stop",
            JSONObject().put("server", JSONObject.NULL).put("reason", "shutdown"),
        ) {
            true
        }
        links?.stop()
        links = null
        bridges?.stop()
        bridges = null
        config = null
        storage = null
        pendingAutostops.clear()
        withBridge.clear()
    }

    /**
     * Binds the bridge server with freshly loaded TLS settings. Returns whether it is listening.
     */
    private fun startBridges(): Boolean {
        val config = config ?: return false
        val server = bridges ?: return false
        val tls = tls ?: return false
        val address = InetSocketAddress(config.bind, config.port)
        return try {
            val ssl =
                tls.load(
                    BridgeTls.parseMode(config.tls) ?: BridgeTls.Mode.AUTO,
                    config.tlsKeystore,
                    config.tlsKeystorePassword,
                )
            server.start(address, ssl)
            log.info(
                "BUSY Bar stream listening on $address (${if (ssl == null) "HTTP" else "HTTPS"}, Tls ${tls.mode.name.lowercase()})"
            )
            true
        } catch (e: IOException) {
            plugin
                .getLogger()
                .log(Level.SEVERE, "Failed to start the BUSY Bar stream on $address", e)
            false
        } catch (e: GeneralSecurityException) {
            plugin
                .getLogger()
                .log(
                    Level.SEVERE,
                    "Failed to set up TLS for the BUSY Bar stream. With Tls auto, delete tls-auto.key and tls-auto.crt " +
                        "from the plugin folder to generate a new certificate; with Tls keystore, check TlsKeystore " +
                        "and TlsKeystorePassword.",
                    e,
                )
            false
        }
    }

    /** Starts accepting backends, unless the secret is unusable. */
    private fun startLinks(config: BusyBarConfig) {
        if (config.linkSecret.length < LinkProtocol.MIN_SECRET_LENGTH) {
            log.warning(
                "BusyBar.Link.Secret is shorter than ${LinkProtocol.MIN_SECRET_LENGTH} characters, so backends " +
                    "cannot report and bridges only see players joining and leaving. Generate one with: " +
                    "openssl rand -base64 32"
            )
            return
        }
        val server =
            BackendLinkServer(plugin.getLogger(), config.linkSecret, plugin.proxy::hasServer, this)
        val address = InetSocketAddress(config.linkBind, config.linkPort)
        try {
            server.start(address)
            links = server
        } catch (e: IOException) {
            plugin
                .getLogger()
                .log(Level.SEVERE, "Failed to start the BUSY Bar backend link on $address", e)
        }
    }

    // ------------------------------------------------------------------ players

    /** Announces [player] joining the network and remembers their name and skin. */
    fun onPlayerJoin(player: UUID, name: String, textures: String?) {
        val storage = storage ?: return
        storage.setName(player, name)
        val head = heads.remember(Heads.skinUrl(textures))
        if (head != null) onlineHeads[player] = head else onlineHeads.remove(player)
        emitPlayers("player.join", name, head, plugin.proxy.players.orEmpty().size)
    }

    /** Announces [player] leaving the network. */
    fun onPlayerQuit(player: UUID, name: String) {
        if (storage == null) return
        val head = onlineHeads.remove(player)
        emitPlayers(
            "player.quit",
            name,
            head,
            plugin.proxy.players.orEmpty().count { it?.uniqueId != player },
        )
    }

    /**
     * Records that [player] is now on [server] and, once the backend has settled the player in,
     * sends their bridges a fresh snapshot with that backend's world, region, and time.
     */
    fun onServerSwitch(player: UUID, server: String) {
        val storage = storage ?: return
        storage.setLastServer(player, server)
        if (player !in withBridge) return
        Thread.ofVirtual().start {
            try {
                Thread.sleep(SWITCH_SNAPSHOT_DELAY_MS)
            } catch (_: InterruptedException) {
                return@start
            }
            val snapshot = snapshot(player)
            bridges?.emit("snapshot", snapshot) { it == player }
        }
    }

    private fun emitPlayers(type: String, name: String, head: String?, online: Int) {
        val data =
            JSONObject()
                .put("player", name)
                .put("online", online)
                .put("max", plugin.proxy.maxPlayers)
                .put("head", head ?: JSONObject.NULL)
        emit(type, data, BridgePermissions.RECEIVE_PLAYERS) { true }
    }

    // ------------------------------------------------------------------ commands

    /** Address bridges connect to, or null when `PublicUrl` is unusable. */
    fun publicUrl(): String? {
        val config = config ?: return null
        val direct = tls?.mode != BridgeTls.Mode.OFF
        return Pairing.resolvePublicUrl(
            config.publicUrl,
            "",
            config.port,
            if (direct) "https" else "http",
            direct,
        )
    }

    /**
     * Issues a new token for [player], replacing and disconnecting any previous one, and returns
     * its pairing string. Null, keeping the current token, while [publicUrl] is unknown.
     */
    fun link(player: UUID): String? {
        val url = publicUrl() ?: return null
        val storage = storage ?: return null
        unlink(player)
        val token = Pairing.newToken()
        val hash = Pairing.hash(token)
        storage.setTokenHash(player, hash)
        tokenIndex[hash] = player
        return Pairing.pairingString(token, url, tls?.pin)
    }

    /** Revokes [player]'s token and closes their bridges. Returns whether one existed. */
    fun unlink(player: UUID): Boolean {
        val storage = storage ?: return false
        val hash = storage.tokenHashes()[player] ?: return false
        storage.setTokenHash(player, null)
        tokenIndex.remove(hash)
        bridges?.disconnect(player)
        return true
    }

    fun isLinked(player: UUID): Boolean = storage?.tokenHashes()?.containsKey(player) == true

    fun connectedCount(player: UUID): Int = bridges?.connectedCount(player) ?: 0

    fun isBusy(player: UUID): Boolean = storage?.busyPlayers()?.contains(player) == true

    /** Players with a token or focus mode on. */
    fun knownPlayers(): Set<UUID> =
        storage?.let { it.tokenHashes().keys + it.busyPlayers() }.orEmpty()

    /** [player]'s last known name, or their UUID. */
    fun nameOf(player: UUID): String = storage?.name(player) ?: player.toString()

    /** The player last seen under [name]. */
    fun playerNamed(name: String): UUID? = storage?.playerNamed(name)

    /**
     * Sets [player]'s focus mode, confirms it to their bridges, and tells every backend so they can
     * tag the player in the tab list.
     */
    fun setBusy(player: UUID, busy: Boolean) {
        val storage = storage ?: return
        if (storage.setBusy(player, busy)) links?.links()?.forEach { it.sendPresence(player, busy) }
        bridges?.emit("presence.updated", JSONObject().put("busy", busy)) { it == player }
    }

    /**
     * Replaces the auto TLS key and restarts the stream, which disconnects every bridge.
     *
     * @return false when TLS is not in auto mode, so there is no key to replace.
     */
    fun rotateTlsKey(): Boolean {
        val tls = tls ?: return false
        if (tls.mode != BridgeTls.Mode.AUTO) return false
        tls.deleteAutoIdentity()
        bridges?.stop()
        startBridges()
        return true
    }

    // ------------------------------------------------------------------ bridges

    override fun playerForToken(token: String): UUID? = tokenIndex[Pairing.hash(token)]

    /**
     * Builds a bridge's snapshot: network-wide counts and focus mode from the proxy, world, region,
     * and autostop from the backend [player] is on, or was last on, when it is linked.
     */
    override fun snapshot(player: UUID): JSONObject {
        val json = JSONObject()
        val server = serverOf(player)
        val link = server?.let { links?.link(it) }
        if (link != null) {
            try {
                val part =
                    link
                        .requestSnapshot(player)
                        .get(BackendLink.REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                BACKEND_FIELDS.filter(part::has).forEach { json.put(it, part.get(it)) }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (_: Exception) {
                // Without the backend's part, the bridge still learns everything the proxy knows.
            }
        }
        json
            .put("online", true)
            .put("players", plugin.proxy.players.orEmpty().size)
            .put("max", plugin.proxy.maxPlayers)
            .put("server", server ?: JSONObject.NULL)
            .put("busy", isBusy(player))
            .put("icon", bridges?.iconTag(player) ?: JSONObject.NULL)
            .put("permissions", JSONArray(grantedNames(player)))
        HIDDEN_FIELDS.forEach { (permission, fields) ->
            if (!has(player, permission)) fields.forEach(json::remove)
        }
        return json
    }

    override fun request(player: UUID, request: JSONObject): Int {
        val config = config ?: return 503
        val action =
            when (request.optString("type")) {
                "presence.set" -> "presence.set"
                "action.invoke" -> request.optString("action")
                else -> return 400
            }
        val permission = BridgePermissions.forAction(action) ?: return 400
        if (action !in config.allowedActions || !has(player, permission)) return 403
        when (action) {
            "presence.set" -> setBusy(player, request.optBoolean("busy"))
            "autostop.abort" -> {
                // Bridges that predate the proxy leave out the server; that is fine while only one
                // is stopping.
                val server =
                    request.optString("server").takeIf { it.isNotEmpty() }
                        ?: pendingAutostops.singleOrNull()
                        ?: return 409
                val link = links?.link(server) ?: return 409
                link.invokeAction(player, "autostop.abort", "busybar:${nameOf(player)}")
            }
        }
        return 202
    }

    /** Tells every backend when [player] gets their first bridge or loses their last one. */
    @Synchronized
    override fun onBridgesChanged(player: UUID, count: Int) {
        val connected = count > 0
        val changed = if (connected) withBridge.add(player) else withBridge.remove(player)
        if (changed) links?.links()?.forEach { it.sendBridge(player, connected) }
    }

    // ------------------------------------------------------------------ backends

    override fun busyPlayers(): Collection<UUID> = storage?.busyPlayers().orEmpty()

    override fun playersWithBridges(): Collection<UUID> = withBridge.toList()

    override fun onConnect(link: BackendLink) {}

    override fun onEvent(link: BackendLink, event: BackendLinkHandler.Event) {
        when (event.type) {
            "autostop.scheduled" -> pendingAutostops += link.server
            "autostop.aborted" -> pendingAutostops -= link.server
        }
        emit(event.type, event.data, event.permission) { player ->
            when (event.scope) {
                BackendLinkHandler.Scope.ALL -> true
                BackendLinkHandler.Scope.PLAYER -> player == event.player
                BackendLinkHandler.Scope.SERVER -> serverOf(player) == link.server
            }
        }
    }

    /**
     * Records reported grants. Permissions can differ between backends, so a report counts when the
     * player is on that backend, or about an offline player when it is the one they were last on;
     * other backends refreshing offline players would otherwise overwrite them.
     */
    override fun onGrants(link: BackendLink, grants: List<BackendLink.Grants>) {
        val storage = storage ?: return
        grants.forEach { (player, granted, online) ->
            if (!online && (serverOf(player) ?: link.server) != link.server) return@forEach
            if (!storage.setGrants(player, granted)) return@forEach
            bridges?.emit(
                "permissions.updated",
                JSONObject().put("granted", JSONArray(grantedNames(player))),
            ) {
                it == player
            }
        }
    }

    /**
     * Tells the bridges of players on a backend that shut down or was lost. A backend that was
     * replaced, reloaded, or is going away with the proxy says nothing.
     */
    override fun onDisconnect(link: BackendLink, reason: String) {
        pendingAutostops -= link.server
        val announced =
            when (reason) {
                "bye: shutdown" -> "shutdown"
                "replaced",
                "proxy stopping" -> return
                else -> if (reason.startsWith("bye: ")) return else "lost"
            }
        emit(
            "server.stop",
            JSONObject().put("server", link.server).put("reason", announced),
            null,
        ) {
            serverOf(it) == link.server
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Queues an event for bridges whose owner hold [permission], if any, and pass [audience]. */
    private fun emit(
        type: String,
        data: JSONObject,
        permission: String?,
        audience: (UUID) -> Boolean,
    ) {
        bridges?.emit(type, data) { player ->
            (permission == null || has(player, permission)) && audience(player)
        }
    }

    /** Bridges get the icon of the backend their owner is on, or was last on. */
    override fun iconKey(player: UUID): String? = serverOf(player)

    /** The backend [player] is on, or was last on while offline. */
    private fun serverOf(player: UUID): String? =
        plugin.proxy.serverOf(player) ?: storage?.lastServer(player)

    /**
     * Whether [player] holds the short [permission], as last reported by a backend. Players no
     * backend has reported yet get the defaults: what everyone has, nothing operators have.
     */
    fun has(player: UUID, permission: String): Boolean =
        storage?.grants(player)?.contains(permission) ?: (permission in BridgePermissions.EVERYONE)

    private fun grantedNames(player: UUID): List<String> =
        BridgePermissions.ALL.filter { has(player, it) }

    /** The bytes of a `data:...;base64,` URL, or null when it is not one. */
    private fun decodeDataUrl(url: String): ByteArray? =
        try {
            Base64.getDecoder().decode(url.substringAfter("base64,"))
        } catch (_: IllegalArgumentException) {
            null
        }

    companion object {
        /** Milliseconds a backend gets to settle a switching player in before the snapshot. */
        private const val SWITCH_SNAPSHOT_DELAY_MS = 1_000L

        /** Snapshot fields that come from the backend. */
        private val BACKEND_FIELDS =
            listOf(
                "tps",
                "mspt",
                "world",
                "ticks",
                "sleeping",
                "sleep_needed",
                "slime_chunk",
                "region",
                "autostop_remaining_s",
            )

        /** Snapshot fields a bridge only sees with the permission. */
        private val HIDDEN_FIELDS =
            mapOf(
                BridgePermissions.RECEIVE_PLAYERS to listOf("players", "max"),
                BridgePermissions.RECEIVE_SERVER to listOf("tps", "mspt"),
                BridgePermissions.RECEIVE_BEDTIME to
                    listOf("world", "ticks", "sleeping", "sleep_needed"),
                BridgePermissions.RECEIVE_SLIMECHUNK to listOf("slime_chunk"),
                BridgePermissions.RECEIVE_REGIONS to listOf("region"),
                BridgePermissions.RECEIVE_AUTOSTOP to listOf("autostop_remaining_s"),
            )
    }
}
