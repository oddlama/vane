package org.oddlama.vane.busybar

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.permissions.Permission
import org.json.JSONObject
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.annotation.config.ConfigStringList
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.busybar.commands.Busybar
import org.oddlama.vane.busybar.common.BridgeLog
import org.oddlama.vane.busybar.common.BridgeTls
import org.oddlama.vane.busybar.common.Heads
import org.oddlama.vane.busybar.common.Pairing
import org.oddlama.vane.busybar.hooks.AdminHook
import org.oddlama.vane.busybar.hooks.BedtimeHook
import org.oddlama.vane.busybar.hooks.PermissionsHook
import org.oddlama.vane.busybar.hooks.PortalsHook
import org.oddlama.vane.busybar.hooks.RegionsHook
import org.oddlama.vane.busybar.link.ProxyLink
import org.oddlama.vane.core.module.Module
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@VaneModule(name = "busybar", configVersion = 1, langVersion = 7, storageVersion = 1)
/**
 * Streams server and vane events to BUSY Bar devices through a small bridge running next to each device.
 *
 * Players link a bridge with `/busybar link`, which hands out a pairing string holding a bearer token
 * and, by default, the fingerprint of the stream's self-signed TLS key (see [Tls]). The bridge keeps an
 * outbound server-sent-events connection open, so the device never has to be reachable from the server.
 *
 * @constructor Creates the module and registers its components.
 */
class BusyBar : Module<BusyBar?>() {
    /** Address the event stream binds to. */
    @ConfigString(
        def = "0.0.0.0",
        desc = "Address the event stream binds to. Use 127.0.0.1 with Tls off when a reverse proxy terminates TLS in front of it."
    )
    var configBindAddress: String? = null

    /** Port of the event stream HTTP server. */
    @ConfigInt(def = 9123, min = 1, max = 65535, desc = "Port of the event stream HTTP server.")
    var configPort: Int = 0

    /** How the event stream is secured, see [Tls]. */
    @ConfigString(
        def = "auto",
        desc = "How the event stream is secured. auto: HTTPS with a self-signed certificate generated on first start; bridges pin its key through the pairing string, so no domain or renewal is needed. keystore: HTTPS with the certificate in TlsKeystore, for example from Let's Encrypt. off: plain HTTP, only for a TLS reverse proxy in front of the port or a trusted LAN."
    )
    var configTls: String? = null

    /** PKCS#12 keystore used when [configTls] is `keystore`. */
    @ConfigString(
        def = "",
        desc = "Path to a PKCS#12 keystore (.p12) with the certificate chain and private key, relative to the server directory. Only used when Tls is keystore. Reload the module after renewing the certificate."
    )
    var configTlsKeystore: String? = null

    /** Password of [configTlsKeystore]. */
    @ConfigString(def = "", desc = "Password of the TLS keystore and its private key.")
    var configTlsKeystorePassword: String? = null

    /** Public base URL shown to players when they link a bridge. */
    @ConfigString(
        def = "",
        desc = "Address bridges connect to, put into the pairing string by /busybar link. A host such as mc.example.com or 203.0.113.7 gets Port added. A full URL is used as is, except that one without a port gets Port unless Tls is off, since bridges then connect to this port directly. Leave empty to use server-ip from server.properties and Port. If both are empty, /busybar link cannot tell players where to connect and asks them to contact an admin. With Tls auto, the address must reach this port directly, since bridges pin this plugin's own certificate; behind a TLS reverse proxy, set Tls to off."
    )
    var configPublicUrl: String? = null

    /** Whether region owners can be told about visitors at all, see [BridgeAccess.receiveRegionVisitors]. */
    @ConfigBoolean(
        def = true,
        desc = "With vane-regions, tell region owners on their BUSY Bar when strangers enter their regions. This reveals other players' locations, so it also needs the vane.busybar.receive.region_visitors permission, which only ops have by default. Set to false to turn it off for everyone."
    )
    var configRegionVisitorAlerts: Boolean = true

    /** Seconds between `server.tps` events. */
    @ConfigInt(def = 10, min = 1, max = 300, desc = "Seconds between server.tps events.")
    var configTpsInterval: Int = 0

    /** Requests for a bridge may send back to the server. */
    @ConfigStringList(
        def = ["presence.set", "autostop.abort"],
        desc = "Requests a linked BUSY Bar may send to the server. Remove an entry to disable it."
    )
    var configAllowedActions: MutableList<String?>? = null

    /** Address of the proxy's BUSY Bar backend link, see [ProxyLink]. */
    @ConfigString(
        def = "",
        desc = "Behind a Velocity proxy running vane-velocity with BUSY Bar support: the address of its BusyBar.Link, as host or host:port (default port 9124). This server then reports its events to the proxy, which serves the bridges and /busybar for the whole network, so this server's own event stream stays off and the stream settings above are ignored. Leave empty when this server is not behind such a proxy."
    )
    var configProxyLinkAddress: String? = null

    /** This server's name as the proxy knows it. */
    @ConfigString(
        def = "",
        desc = "This server's name in the proxy's velocity.toml, for example survival. Required with ProxyLinkAddress."
    )
    var configProxyLinkServerName: String? = null

    /** Secret shared with the proxy's `BusyBarLink.Secret`. */
    @ConfigString(
        def = "",
        desc = "Same value as BusyBar.Link.Secret in the proxy's vane-velocity config.toml, at least 16 characters. Required with ProxyLinkAddress. It is never sent over the network."
    )
    var configProxyLinkSecret: String? = null

    /** SHA-256 hashes of the bridge token issued to each player. */
    @Persistent
    var storageTokenHashes: MutableMap<UUID?, String?> = mutableMapOf()

    /** Token hash to player lookup, read from HTTP threads. */
    private val tokenIndex = ConcurrentHashMap<String, UUID>()

    /** [log] for the shared BUSY Bar code. */
    val bridgeLog = BridgeLog { level, message, error -> log.log(level, message, error) }

    /** Permissions deciding what each bridge receives and may request. */
    val access = BridgeAccess(this)

    /** TLS mode, certificate, and key fingerprint of the event stream. */
    val tls = Tls(this)

    /** Link reporting this server to the proxy, when configured. */
    val proxyLink = ProxyLink(this)

    /** Event stream HTTP server and connected bridges. */
    val stream = EventStream(this)

    /** Player faces served by `/head`. */
    val heads = Heads()

    /** Focus-mode state reported by the bridges. */
    val presence = Presence(this)

    /** Advancements, batched per player. */
    val advancements = Advancements(this)

    /** Slime chunk reports for players carrying a slime bucket. */
    val slimeChunks = SlimeChunks(this)

    /** Vanilla server events. */
    val serverEvents = ServerEvents(this)

    /** vane-admin integration, present while vane-admin is loaded. */
    var adminHook: AdminHook? = null
        private set

    /** vane-bedtime integration, present while vane-bedtime is loaded. */
    var bedtimeHook: BedtimeHook? = null
        private set

    /** vane-regions integration, present while vane-regions are loaded. */
    var regionsHook: RegionsHook? = null
        private set

    /** vane-permissions integration, present while vane-permissions is loaded. */
    var permissionsHook: PermissionsHook? = null
        private set

    /** Bukkit listeners registered for optional integrations. */
    private val hookListeners = mutableListOf<Listener>()

    init {
        Busybar(this)
    }

    /** Rebuilds the token index and attaches the optional integrations that are installed. */
    override fun onModuleEnable() {
        tokenIndex.clear()
        storageTokenHashes.forEach { (player, hash) -> if (player != null && hash != null) tokenIndex[hash] = player }
        // Behind a proxy, the proxy serves bridges and checks its own TLS and address.
        if (!proxied()) {
            tls.checkConfiguration()
            if (publicUrl() == null) {
                val problem = if (configPublicUrl.isNullOrBlank()) "Neither PublicUrl nor server-ip is set"
                else "PublicUrl '$configPublicUrl' is not a valid address"
                log.warning(
                    "$problem, so /busybar link cannot tell players where to connect. Set PublicUrl in the " +
                        "vane-busybar config to the address bridges reach this server at, for example " +
                        "mc.example.com or 203.0.113.7."
                )
            }
        }

        val plugins = server.pluginManager
        plugins.getPlugin("vane-admin")?.takeIf { it.isEnabled }?.let { adminHook = AdminHook.create(this, it)?.also(AdminHook::start) }
        plugins.getPlugin("vane-bedtime")?.takeIf { it.isEnabled }?.let { bedtimeHook = BedtimeHook.create(it) }
        plugins.getPlugin("vane-permissions")?.takeIf { it.isEnabled }?.let { permissionsHook = PermissionsHook.create(it) }
        plugins.getPlugin("vane-portals")?.takeIf { it.isEnabled }?.let { hookListeners += PortalsHook(this) }
        plugins.getPlugin("vane-regions")?.takeIf { it.isEnabled }?.let { regionsHook = RegionsHook.create(this, it)?.also(RegionsHook::start) }
        hookListeners.forEach(::registerListener)
    }

    /** Detaches all optional integrations. */
    override fun onModuleDisable() {
        adminHook?.stop()
        adminHook = null
        regionsHook?.stop()
        regionsHook = null
        bedtimeHook = null
        permissionsHook = null
        hookListeners.forEach(HandlerList::unregisterAll)
        hookListeners.clear()
    }

    /** Issues a new bridge token for [player], replacing and disconnecting any previous one. */
    fun issueToken(player: UUID): String {
        revokeToken(player)
        val token = Pairing.newToken()
        val hash = Pairing.hash(token)
        storageTokenHashes[player] = hash
        tokenIndex[hash] = player
        markPersistentStorageDirty()
        return token
    }

    /** Revokes the token of [player] and closes their bridge connections. Returns whether one existed. */
    fun revokeToken(player: UUID): Boolean {
        val hash = storageTokenHashes.remove(player) ?: return false
        tokenIndex.remove(hash)
        markPersistentStorageDirty()
        stream.disconnect(player)
        return true
    }

    /** Players with a linked bridge token. */
    fun linkedPlayers(): List<UUID> = storageTokenHashes.keys.filterNotNull()

    /** Whether [player] has a linked bridge token. */
    fun isLinked(player: UUID): Boolean = storageTokenHashes.containsKey(player)

    /** Resolves a bearer token to its player. Safe to call from any thread. */
    fun playerForToken(token: String): UUID? = tokenIndex[Pairing.hash(token)]

    /** Whether a proxy serves the bridges, see [ProxyLink]. */
    fun proxied(): Boolean = !configProxyLinkAddress.isNullOrBlank()

    /** Whether [player] has a bridge connected, here or, behind a proxy, there. */
    fun hasBridge(player: UUID): Boolean =
        if (proxied()) proxyLink.hasBridge(player) else stream.connectedCount(player) > 0

    /** Remembers [player]'s skin for `/head` and returns its id, or null without a skin. */
    fun headOf(player: Player): String? = heads.remember(player.playerProfile.textures.skin)

    /**
     * URL players should point their bridge at, see [Pairing.resolvePublicUrl]. Uses plain HTTP only when TLS
     * is off. Null when the address is unknown or [configPublicUrl] is invalid.
     */
    fun publicUrl(): String? {
        val direct = tls.mode != BridgeTls.Mode.OFF
        return Pairing.resolvePublicUrl(configPublicUrl, server.ip, configPort, if (direct) "https" else "http", direct)
    }

    /**
     * Everything a bridge needs in one line: [token], the address from [publicUrl] and, in auto TLS
     * mode, the key fingerprint to pin, for example `<token>@203.0.113.7:9123#sha256=<base64url>`.
     * Bridges assume https, so only plain HTTP keeps its prefix, which a bridge must never
     * fall back to on its own. Null while [publicUrl] is unknown.
     */
    fun pairingString(token: String): String? = publicUrl()?.let { Pairing.pairingString(token, it, tls.pin) }

    /**
     * Replaces the auto TLS key and restarts the event stream, which disconnects every bridge. Bridges
     * then reject the new key until their owner links again. Must run on the main thread.
     *
     * @return false when TLS is not in auto mode, so there is no key to replace.
     */
    fun rotateTlsKey(): Boolean {
        if (tls.mode != BridgeTls.Mode.AUTO) return false
        tls.deleteAutoIdentity()
        stream.restart()
        return true
    }

    /** Whether the bridge request [action] is enabled in the configuration. */
    fun actionAllowed(action: String): Boolean = configAllowedActions?.contains(action) == true

    /** Permission required for the bridge request [action], or null for unknown requests. */
    fun permissionForAction(action: String): Permission? = when (action) {
        "presence.set" -> access.actionPresence
        "autostop.abort" -> access.actionAutostopAbort
        else -> null
    }

    /**
     * Builds the state a bridge receives right after connecting. Must run on the main thread.
     *
     * @param player the player owning the connecting bridge.
     */
    fun snapshot(player: UUID): JSONObject = JSONObject().also { json ->
        serverEvents.contributeSnapshot(json, player)
        if (!access.has(player, access.receivePlayers)) listOf("players", "max").forEach(json::remove)
        if (!access.has(player, access.receiveServer)) listOf("tps", "mspt").forEach(json::remove)
        if (!access.has(player, access.receiveBedtime)) listOf("world", "ticks", "sleeping", "sleep_needed").forEach(json::remove)
        json.put("busy", presence.isBusy(player))
        val online = server.getPlayer(player)
        if (online != null && access.has(player, access.receiveSlimeChunk)) {
            json.put("slime_chunk", slimeChunks.active(online))
        }
        if (access.has(player, access.receiveRegions)) {
            json.put("region", online?.let { regionsHook?.describeCurrent(it) } ?: JSONObject.NULL)
        }
        json.put("icon", stream.iconTag() ?: JSONObject.NULL)
        json.put("permissions", access.grantedNames(player))
        if (access.has(player, access.receiveAutostop)) {
            json.put("autostop_remaining_s", adminHook?.remainingSeconds() ?: JSONObject.NULL)
        }
    }

    /**
     * Executes a request sent by a bridge. Runs on the main thread.
     *
     * @param player the player owning the bridge.
     * @param request the parsed request body.
     */
    fun handleRequest(player: UUID, request: JSONObject) {
        when (request.optString("type")) {
            "presence.set" -> if (access.has(player, access.actionPresence)) {
                presence.setBusy(player, request.optBoolean("busy"))
            }
            "action.invoke" -> when (request.optString("action")) {
                "autostop.abort" -> if (access.has(player, access.actionAutostopAbort)) {
                    adminHook?.abort("busybar:${server.getOfflinePlayer(player).name}")
                }
            }
        }
    }
}
