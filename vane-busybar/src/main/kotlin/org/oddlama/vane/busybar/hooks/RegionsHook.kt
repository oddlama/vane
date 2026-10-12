package org.oddlama.vane.busybar.hooks

import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.json.JSONArray
import org.json.JSONObject
import org.oddlama.vane.busybar.BusyBar
import org.oddlama.vane.busybar.VisitorAlerts
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.EnvironmentSetting
import org.oddlama.vane.regions.region.Region
import org.oddlama.vane.regions.region.RoleSetting
import java.util.*

/**
 * Tells players which vane-regions region they are in, and region owners who visits theirs.
 *
 * vane-regions has no enter or leave events, so every online player's region is looked up once
 * per second. The lookup is indexed by chunk, so this stays cheap on busy servers.
 *
 * @param busybar owning module.
 * @param regions the vane-regions module.
 */
class RegionsHook private constructor(private val busybar: BusyBar, private val regions: Regions) {
    /** Repeating the poll task. */
    private var task: BukkitTask? = null

    /** The region each online player was in at the last poll. */
    private val current = mutableMapOf<UUID, Region>()

    /** Whether [current] was filled once, so players already inside are not reported as entering. */
    private var primed = false

    /** Batches visitor reports per region. */
    private val visitors = VisitorAlerts { alert ->
        busybar.stream.emit(
            "region.visitor",
            JSONObject()
                .put("region", alert.regionName)
                .put("visitors", JSONArray(alert.visitors))
                .put("count", alert.count)
                .put("head", alert.head ?: JSONObject.NULL),
            alert.owner,
            busybar.access.receiveRegionVisitors
        )
    }

    /** Starts polling. */
    fun start() {
        primed = false
        task = busybar.scheduleTaskTimer({ poll() }, POLL_TICKS, POLL_TICKS)
    }

    /** Stops polling and forgets pending visitors. */
    fun stop() {
        task?.cancel()
        task = null
        current.clear()
        visitors.clear()
    }

    /** The region [player] is in, as sent in `region.enter`, or null outside of regions. */
    fun describeCurrent(player: Player): JSONObject? = regions.regionAt(player.location)?.let { describe(it, player) }

    /** Looks up every online player's region and reports changes. */
    private fun poll() {
        val now = System.currentTimeMillis()
        visitors.flush(now)
        val online = busybar.server.onlinePlayers
        current.keys.retainAll(online.mapTo(HashSet()) { it.uniqueId })

        for (player in online) {
            val region = regions.regionAt(player.location)
            val previous = current[player.uniqueId]
            if (region?.id() == previous?.id()) continue
            if (region == null) current.remove(player.uniqueId) else current[player.uniqueId] = region
            if (!primed) continue

            if (previous != null) {
                busybar.stream.emit(
                    "region.leave",
                    JSONObject().put("region", previous.name() ?: UNNAMED),
                    player.uniqueId,
                    busybar.access.receiveRegions
                )
            }
            if (region != null) {
                busybar.stream.emit("region.enter", describe(region, player), player.uniqueId, busybar.access.receiveRegions)
                reportVisitor(now, region, player)
            }
        }
        primed = true
    }

    /** Hands [player] to [visitors] when they are a stranger in [region] and its owner wants to know. */
    private fun reportVisitor(now: Long, region: Region, player: Player) {
        if (!busybar.configRegionVisitorAlerts) return
        val owner = region.owner() ?: return
        val id = region.id() ?: return
        if (owner == player.uniqueId) return
        // Members with build rights come and go all the time; only strangers ring the Bar.
        if (region.regionGroup(regions)?.getRole(player.uniqueId)?.getSetting(RoleSetting.BUILD) == true) return
        // An owner standing in the region sees their visitors already.
        if (current[owner]?.id() == id) return
        if (!busybar.hasBridge(owner) || !busybar.access.has(owner, busybar.access.receiveRegionVisitors)) return
        visitors.visit(now, id, region.name() ?: UNNAMED, owner, player.uniqueId, player.name, busybar.headOf(player))
    }

    /** What a player's bridge learns about [region] on entering it. */
    private fun describe(region: Region, player: Player): JSONObject {
        val group = region.regionGroup(regions)
        val owner = region.owner()
        return JSONObject()
            .put("region", region.name() ?: UNNAMED)
            .put("owner", owner?.let { busybar.server.getOfflinePlayer(it).name } ?: JSONObject.NULL)
            .put("own", owner == player.uniqueId)
            .put("pvp", group?.getSetting(EnvironmentSetting.PVP) ?: EnvironmentSetting.PVP.defaultValue())
            .put("may_build", owner == player.uniqueId || group?.getRole(player.uniqueId)?.getSetting(RoleSetting.BUILD) == true)
    }

    companion object {
        /** Ticks between polls. */
        private const val POLL_TICKS = 20L

        /** Name used for regions without one. */
        private const val UNNAMED = "Region"

        /** Creates the hook when [plugin] is vane-regions. */
        fun create(busybar: BusyBar, plugin: Plugin): RegionsHook? = (plugin as? Regions)?.let { RegionsHook(busybar, it) }
    }
}
