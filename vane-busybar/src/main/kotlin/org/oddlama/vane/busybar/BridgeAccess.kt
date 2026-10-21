package org.oddlama.vane.busybar

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.bukkit.scheduler.BukkitTask
import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.busybar.common.BridgePermissions
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

/**
 * Decides which events a bridge receives and which requests it may send, using regular permissions.
 *
 * Bridges stay connected while their owner is offline, but Bukkit can only evaluate permissions of
 * online players. The grants of each player are therefore snapshotted while they are online (on
 * join, on quit, and once a minute) and used while they are away. With vane-permissions installed,
 * offline players are re-evaluated from their stored groups once a minute as well; with any other
 * permission plugin, changes made while a player is offline apply when they rejoin.
 *
 * @param context owning context.
 */
class BridgeAccess(context: Context<BusyBar?>) : Listener<BusyBar?>(context) {
    private val busybar: BusyBar
        get() = requireNotNull(module)

    /** Receive `player.join` and `player.quit`. */
    val receivePlayers =
        permission(
            BridgePermissions.RECEIVE_PLAYERS,
            "Receive players joining and leaving",
            PermissionDefault.TRUE,
        )

    /** Receive `bedtime.*` and `world.time`. */
    val receiveBedtime =
        permission(
            BridgePermissions.RECEIVE_BEDTIME,
            "Receive sleeping players, night skips and world time",
            PermissionDefault.TRUE,
        )

    /** Receive the player's own `portal.*` events. */
    val receivePortals =
        permission(
            BridgePermissions.RECEIVE_PORTALS,
            "Receive portal use and destruction of your own portals",
            PermissionDefault.TRUE,
        )

    /** Receive `advancement`. */
    val receiveAdvancements =
        permission(
            BridgePermissions.RECEIVE_ADVANCEMENTS,
            "Receive advancements players make",
            PermissionDefault.TRUE,
        )

    /** Receive `slimechunk.*` while carrying a slime bucket. */
    val receiveSlimeChunk =
        permission(
            BridgePermissions.RECEIVE_SLIMECHUNK,
            "Receive entering and leaving slime chunks while carrying a slime bucket",
            PermissionDefault.TRUE,
        )

    /** Receive `region.enter` and `region.leave` for the regions the player walks through. */
    val receiveRegions =
        permission(
            BridgePermissions.RECEIVE_REGIONS,
            "Receive entering and leaving vane-regions regions",
            PermissionDefault.TRUE,
        )

    /** Receive `region.visitor` when strangers enter the player's own regions. */
    val receiveRegionVisitors =
        permission(
            BridgePermissions.RECEIVE_REGION_VISITORS,
            "Receive who enters your vane-regions regions, which reveals other players' locations",
            PermissionDefault.OP,
        )

    /** Receive `server.tps`. */
    val receiveServer =
        permission(
            BridgePermissions.RECEIVE_SERVER,
            "Receive server performance (TPS and MSPT)",
            PermissionDefault.OP,
        )

    /** Receive `autostop.*`. */
    val receiveAutostop =
        permission(
            BridgePermissions.RECEIVE_AUTOSTOP,
            "Receive the autostop countdown",
            PermissionDefault.OP,
        )

    /** Send `presence.set`. */
    val actionPresence =
        permission(
            BridgePermissions.ACTION_PRESENCE,
            "Set focus mode from the BUSY Bar",
            PermissionDefault.TRUE,
        )

    /** Send `action.invoke` for `autostop.abort`. */
    val actionAutostopAbort =
        permission(
            BridgePermissions.ACTION_AUTOSTOP_ABORT,
            "Abort autostop from the BUSY Bar",
            PermissionDefault.OP,
        )

    /** Every permission above, in a stable order. */
    private val all =
        listOf(
            receivePlayers,
            receiveBedtime,
            receivePortals,
            receiveAdvancements,
            receiveSlimeChunk,
            receiveRegions,
            receiveRegionVisitors,
            receiveServer,
            receiveAutostop,
            actionPresence,
            actionAutostopAbort,
        )

    /** Last known grants per player, persisted for offline evaluation. */
    @Persistent var storageGrants: MutableMap<UUID?, MutableSet<String?>?> = mutableMapOf()

    /** Thread-safe copy of [storageGrants], read by HTTP threads. */
    private val grants = ConcurrentHashMap<UUID, Set<String>>()

    /** Periodic refresh task. */
    private var task: BukkitTask? = null

    init {
        val module = busybar
        all.forEach(module::registerPermission)
        module.registerPermission(wildcard("receive", "Receive every BUSY Bar event"))
        module.registerPermission(wildcard("action", "Send every BUSY Bar request"))
    }

    /** Loads stored grants and starts the periodic refresh. */
    override fun onEnable() {
        super.onEnable()
        grants.clear()
        storageGrants.forEach { (player, names) ->
            if (player != null && names != null) grants[player] = names.filterNotNull().toSet()
        }
        task = scheduleTaskTimer({ refreshAll() }, 1L, REFRESH_TICKS)
    }

    /** Stops the periodic refresh. */
    override fun onDisable() {
        task?.cancel()
        task = null
        super.onDisable()
    }

    /**
     * Refreshes grants once permission plugins had a tick to attach theirs (vane-permissions
     * attaches at MONITOR). Always reports them to the proxy, which may not know them yet even when
     * they are unchanged here; it ignores reports that change nothing.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        scheduleNextTick {
            if (!player.isOnline) return@scheduleNextTick
            refresh(player)
            busybar.proxyLink.sendGrants(
                player.uniqueId,
                grantedNames(player.uniqueId),
                online = true,
            )
        }
    }

    /**
     * Snapshots grant one last time before the player goes offline. Runs at LOWEST because
     * permission plugins such as vane-permissions drop their attachment at MONITOR.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) = refresh(event.player)

    /**
     * Whether [player] holds [permission], according to the last snapshot of their grants. Safe to
     * call from any thread.
     */
    fun has(player: UUID, permission: Permission): Boolean {
        grants[player]?.let {
            return permission.name in it
        }
        // Never seen online since linking: fall back to the permission's default.
        return permission.default.getValue(busybar.server.getOfflinePlayer(player).isOp)
    }

    /** Short names of the permissions [player] holds, as sent to bridges. */
    fun grantedNames(player: UUID): JSONArray =
        JSONArray(all.filter { has(player, it) }.map { shortName(it) })

    /**
     * Re-evaluates every linked player: online players live, offline players through
     * vane-permissions when it is installed, otherwise they keep their last snapshot.
     */
    private fun refreshAll() {
        val server = busybar.server
        server.onlinePlayers.forEach(::refresh)
        val hook = busybar.permissionsHook ?: return
        busybar
            .linkedPlayers()
            .filter { server.getPlayer(it) == null }
            .forEach { id ->
                val isOp = server.getOfflinePlayer(id).isOp
                store(id, all.filter { hook.has(id, isOp, it) }.map { it.name }.toSet())
            }
    }

    /** Re-evaluates the grants of an online [player]. */
    fun refresh(player: Player) =
        store(player.uniqueId, all.filter(player::hasPermission).map { it.name }.toSet())

    /** Records the grants of [id] and tells their bridge and the proxy when they changed. */
    private fun store(id: UUID, now: Set<String>) {
        if (grants.put(id, now) == now) return
        storageGrants[id] = now.toMutableSet()
        markPersistentStorageDirty()
        val names = grantedNames(id)
        busybar.stream.emit("permissions.updated", JSONObject().put("granted", names), id)
        busybar.proxyLink.sendGrants(id, names, busybar.server.getPlayer(id) != null)
    }

    /** Creates a permission below `vane.busybar.`. */
    private fun permission(suffix: String, description: String, default: PermissionDefault) =
        Permission("vane.${busybar.annotationName}.$suffix", description, default)

    /** Creates the `.*` parent of every permission in [group]. */
    private fun wildcard(group: String, description: String): Permission {
        val prefix = "vane.${busybar.annotationName}.$group."
        val children = all.filter { it.name.startsWith(prefix) }.associate { it.name to true }
        return Permission("$prefix*", description, PermissionDefault.OP, children)
    }

    /** Name without the `vane.busybar.` prefix, for example `receive.autostop`. */
    fun shortName(permission: Permission) =
        permission.name.removePrefix("vane.${busybar.annotationName}.")

    companion object {
        /** Ticks between refreshes of online players. */
        private const val REFRESH_TICKS = 60L * 20L
    }
}
