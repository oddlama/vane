package org.oddlama.vane.busybar.link

import org.bukkit.permissions.Permission
import org.bukkit.plugin.IllegalPluginAccessException
import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.busybar.BusyBar
import org.oddlama.vane.busybar.common.LinkProtocol
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

/**
 * Reports this backend to the proxy's BUSY Bar backend link, when `ProxyLinkAddress` is set. The
 * proxy then serves the bridges, so this server's own event stream stays off.
 *
 * Every event the module emits is forwarded with who should receive it, and the grants recorded by
 * [org.oddlama.vane.busybar.BridgeAccess] go along, so the proxy can filter events for bridges
 * whose owners are elsewhere on the network. The proxy in turn asks for snapshots, runs actions,
 * and reports focus-mode changes and which players have a bridge connected.
 *
 * @param context owning context.
 */
class ProxyLink(context: Context<BusyBar?>) : ModuleComponent<BusyBar?>(context), ProxyLinkClient.Handler {
    private val busybar: BusyBar
        get() = requireNotNull(module)

    /** The running client, while configured. */
    private var client: ProxyLinkClient? = null

    /** Players with a bridge connected to the proxy, as last reported by it. */
    private val bridges: MutableSet<UUID> = ConcurrentHashMap.newKeySet()

    /** Whether [player] has a bridge connected to the proxy. Safe to call from any thread. */
    fun hasBridge(player: UUID): Boolean = player in bridges

    /** Whether the link to the proxy is established. */
    val linked: Boolean
        get() = client?.linked == true

    /** Validates the settings and starts linking; logs and stays off when they are incomplete. */
    override fun onEnable() {
        val configured = busybar.configProxyLinkAddress?.trim().orEmpty()
        if (configured.isEmpty()) return
        val address = parseAddress(configured)
        val server = busybar.configProxyLinkServerName?.trim().orEmpty()
        val secret = busybar.configProxyLinkSecret.orEmpty()
        val problem = when {
            address == null -> "ProxyLinkAddress '$configured' is not a host or host:port"
            server.isEmpty() -> "ProxyLinkServerName is empty; set it to this server's name in velocity.toml"
            secret.length < LinkProtocol.MIN_SECRET_LENGTH ->
                "ProxyLinkSecret must be at least ${LinkProtocol.MIN_SECRET_LENGTH} characters"
            else -> null
        }
        if (problem != null) {
            return busybar.log.severe("$problem, so the proxy link stays off and no BUSY Bar is served.")
        }

        client = ProxyLinkClient(
            requireNotNull(address), server, secret, busybar.pluginMeta.version, capabilities(), this, busybar.log
        ).also(ProxyLinkClient::start)
    }

    /** Says goodbye to the proxy. */
    override fun onDisable() {
        val stopping = busybar.server.isStopping
        client?.stop(if (stopping) "shutdown" else "disabled")
        client = null
        bridges.clear()
    }

    /**
     * Forwards an event the module emitted. Events the proxy produces itself, from its own view of
     * players and bridges, are left out.
     *
     * @param target the only receiving player, or null for a broader audience.
     * @param permission what receivers need, or null for none.
     */
    fun forward(type: String, data: JSONObject, target: UUID?, permission: Permission?) {
        val current = client ?: return
        if (type in PROXY_OWNED) return
        val to = when {
            target != null -> JSONObject().put("scope", "player").put("player", target.toString())
            type in SERVER_SCOPED || SERVER_SCOPED_PREFIXES.any(type::startsWith) -> JSONObject().put("scope", "server")
            else -> JSONObject().put("scope", "all")
        }
        val frame = JSONObject().put("t", "event").put("type", type).put("to", to).put("data", data)
        permission?.let { frame.put("perm", busybar.access.shortName(it)) }
        current.send(frame)
    }

    /**
     * Reports the short permission names [player] now holds. [online] says whether they are on
     * this server; the proxy only takes reports about offline players from the server they were
     * last on, since permissions can differ between servers.
     */
    fun sendGrants(player: UUID, granted: JSONArray, online: Boolean) {
        client?.send(
            JSONObject().put("t", "grants").put("player", player.toString()).put("granted", granted).put("online", online)
        )
    }

    /** Catches up with the proxy: it learns everyone's grants, this server its focus modes and bridges. */
    override fun onLinked(busy: Set<UUID>, bridges: Set<UUID>) {
        this.bridges.retainAll(bridges)
        this.bridges.addAll(bridges)
        onMainThread {
            val grants = busybar.server.onlinePlayers.map {
                JSONObject().put("player", it.uniqueId.toString()).put("granted", busybar.access.grantedNames(it.uniqueId))
                    .put("online", true)
            }
            client?.send(JSONObject().put("t", "grants.bulk").put("grants", JSONArray(grants)))
            // The proxy owns focus mode; this server only mirrors it for the tab list tag.
            busy.filterNot(busybar.presence::isBusy).forEach { busybar.presence.setBusy(it, true) }
            busybar.presence.busyPlayers().filterNot(busy::contains).forEach { busybar.presence.setBusy(it, false) }
        }
    }

    /** Without the proxy nobody receives anything, so stop looking for bridges' events. */
    override fun onUnlinked() = bridges.clear()

    override fun onBridge(player: UUID, connected: Boolean) {
        if (connected) bridges += player else bridges -= player
    }

    override fun onPresence(player: UUID, busy: Boolean) {
        onMainThread { busybar.presence.setBusy(player, busy) }
    }

    override fun snapshot(player: UUID, reply: (JSONObject?) -> Unit) {
        val scheduled = onMainThread {
            reply(
                try {
                    busybar.snapshot(player)
                } catch (e: RuntimeException) {
                    busybar.log.log(Level.WARNING, "Could not build a BUSY Bar snapshot for the proxy", e)
                    null
                }
            )
        }
        if (!scheduled) reply(null)
    }

    override fun action(player: UUID, action: String, by: String, reply: (Boolean) -> Unit) {
        val scheduled = onMainThread {
            val hook = busybar.adminHook
            reply(
                when {
                    action == "autostop.abort" && hook != null -> {
                        hook.abort(by.ifEmpty { "busybar:proxy" })
                        true
                    }
                    else -> false
                }
            )
        }
        if (!scheduled) reply(false)
    }

    /** Runs [task] on the main thread; returns false when the module can no longer schedule. */
    private fun onMainThread(task: () -> Unit): Boolean = try {
        busybar.server.scheduler.runTask(busybar, Runnable(task))
        true
    } catch (_: IllegalPluginAccessException) {
        // The module is being disabled.
        false
    }

    /** Optional integrations installed here, so the proxy knows which snapshot fields to expect. */
    private fun capabilities(): Set<String> {
        val plugins = busybar.server.pluginManager
        return INTEGRATIONS.filterValues(plugins::isPluginEnabled).keys
    }

    companion object {
        /** Default port of the proxy's `BusyBar.Link`. */
        private const val DEFAULT_PORT = 9124

        /** Events the proxy produces itself. */
        private val PROXY_OWNED = setOf("player.join", "player.quit", "server.stop", "presence.updated", "permissions.updated")

        /** Events about this server, for bridges whose owner is on it. */
        private val SERVER_SCOPED = setOf("server.tps")
        private val SERVER_SCOPED_PREFIXES = listOf("bedtime.", "world.")

        /** Capability name by plugin. */
        private val INTEGRATIONS = mapOf(
            "admin" to "vane-admin",
            "bedtime" to "vane-bedtime",
            "permissions" to "vane-permissions",
            "portals" to "vane-portals",
            "regions" to "vane-regions",
            "slimechunk" to "vane-trifles"
        )

        /**
         * Parses `host`, `host:port` or `[ipv6]:port`. Null when invalid. The host stays unresolved;
         * the client looks it up on every connection attempt.
         */
        fun parseAddress(value: String): InetSocketAddress? {
            val match = Regex("""^(\[[^]]+]|[^:\[\]/\s]+)(?::(\d{1,5}))?$""").matchEntire(value)
            val host = match?.groupValues?.get(1)?.removeSurrounding("[", "]")
                ?: value.takeIf { ':' in it && '/' !in it && ' ' !in it } // bare IPv6
                ?: return null
            val port = match?.groupValues?.get(2)?.takeIf { it.isNotEmpty() }?.toInt() ?: DEFAULT_PORT
            if (port !in 1..65535) return null
            return InetSocketAddress.createUnresolved(host, port)
        }
    }
}
