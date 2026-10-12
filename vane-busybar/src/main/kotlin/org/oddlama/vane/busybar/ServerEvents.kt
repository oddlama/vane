package org.oddlama.vane.busybar

import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitTask
import org.json.JSONObject
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import java.util.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Forwards vanilla server state: players joining and leaving, sleeping, time of day and tick rate.
 *
 * Sleep requirements follow vane-bedtime's threshold when it is installed, otherwise every
 * non-spectator in the world has to sleep.
 *
 * @param context owning context.
 */
class ServerEvents(context: Context<BusyBar?>) : Listener<BusyBar?>(context) {
    private val busybar: BusyBar
        get() = requireNotNull(module)

    /** Per-world state from the previous poll. */
    private class WorldState(val fullTime: Long, val night: Boolean, val sleeping: Int)

    /** Last polled state per overworld-type world. */
    private val worlds = mutableMapOf<UUID, WorldState>()

    /** Repeating the poll task. */
    private var task: BukkitTask? = null

    /** Seconds since the last `server.tps` event. */
    private var secondsSinceTps = 0

    /** Seconds since the last `world.time` event. */
    private var secondsSinceTimeSync = 0

    /** Registers the listener and starts polling once per second. */
    override fun onEnable() {
        super.onEnable()
        task = scheduleTaskTimer({ poll() }, POLL_TICKS, POLL_TICKS)
    }

    /** Stops polling and unregisters the listener. */
    override fun onDisable() {
        task?.cancel()
        task = null
        worlds.clear()
        super.onDisable()
    }

    /** Announces a join to every bridge. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        busybar.stream.emit(
            "player.join",
            playerCount(event.player, busybar.server.onlinePlayers.size),
            permission = busybar.access.receivePlayers
        )
    }

    /**
     * Announces quit to every bridge. The quitting player is still counted online during this
     * event. During shutdown Paper kicks everyone before plugins stop; those kicks are reported as
     * a single `server.stop` instead of one quit per player.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (busybar.server.isStopping) return busybar.stream.announceStop()
        busybar.stream.emit(
            "player.quit",
            playerCount(event.player, busybar.server.onlinePlayers.size - 1),
            permission = busybar.access.receivePlayers
        )
    }

    /** Announces a player going to bed, once they are actually asleep. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBedEnter(event: PlayerBedEnterEvent) {
        if (event.bedEnterResult != PlayerBedEnterEvent.BedEnterResult.OK) return
        val player = event.player
        scheduleNextTick { if (player.isOnline) emitBedtime("bedtime.enter", player) }
    }

    /** Announces a player leaving their bed. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onBedLeave(event: PlayerBedLeaveEvent) {
        val player = event.player
        scheduleNextTick { if (player.isOnline) emitBedtime("bedtime.leave", player) }
    }

    /** Adds the vanilla part of the connection snapshot for [player]. */
    fun contributeSnapshot(json: JSONObject, player: UUID) {
        val server = busybar.server
        val world = server.getPlayer(player)?.world?.takeIf { it.environment == World.Environment.NORMAL }
            ?: server.worlds.first()
        json.put("online", true)
            .put("players", server.onlinePlayers.size)
            .put("max", server.maxPlayers)
            .put("tps", tps())
            .put("mspt", mspt())
            .put("world", world.name)
            .put("ticks", world.time)
            .put("sleeping", sleeping(world))
            .put("sleep_needed", sleepNeeded(world))
    }

    /** Polls tick rate, time jumps and night skips. */
    private fun poll() {
        if (++secondsSinceTps >= busybar.configTpsInterval) {
            secondsSinceTps = 0
            busybar.stream.emit(
                "server.tps",
                JSONObject().put("tps", tps()).put("mspt", mspt()),
                permission = busybar.access.receiveServer
            )
        }

        // Bridges extrapolate the clock at 20 ticks per second. Gradual changes such as
        // vane-admin's smooth /time never count as a jump, so resend the time now and then.
        val resync = ++secondsSinceTimeSync >= TIME_SYNC_SECONDS
        if (resync) secondsSinceTimeSync = 0

        for (world in busybar.server.worlds) {
            if (world.environment != World.Environment.NORMAL) continue
            val now = WorldState(world.fullTime, !world.isDayTime, sleeping(world))
            val before = worlds.put(world.uid, now) ?: continue

            val jumped = abs(now.fullTime - before.fullTime - POLL_TICKS) > TIME_JUMP_TOLERANCE
            if (jumped || resync) {
                busybar.stream.emit(
                    "world.time",
                    JSONObject().put("world", world.name).put("ticks", world.time),
                    permission = busybar.access.receiveBedtime
                )
            }
            if (before.night && !now.night && before.sleeping > 0) {
                busybar.stream.emit(
                    "bedtime.skip",
                    JSONObject().put("world", world.name).put("ticks", world.time),
                    permission = busybar.access.receiveBedtime
                )
            }
        }
    }

    /** Emits a bedtime event for [player]'s world. */
    private fun emitBedtime(type: String, player: Player) {
        val world = player.world
        busybar.stream.emit(
            type,
            JSONObject()
                .put("world", world.name)
                .put("player", player.name)
                .put("sleeping", sleeping(world))
                .put("needed", sleepNeeded(world)),
            permission = busybar.access.receiveBedtime
        )
    }

    /** Payload for join and quit events. `head` is the skin id for `/head/<id>`, or null without a skin. */
    private fun playerCount(player: Player, online: Int): JSONObject =
        JSONObject()
            .put("player", player.name)
            .put("online", online)
            .put("max", busybar.server.maxPlayers)
            .put("head", busybar.headOf(player) ?: JSONObject.NULL)

    /** Number of sleeping players in [world]. */
    private fun sleeping(world: World): Int = world.players.count { it.isSleeping }

    /** Number of sleeping players required to skip the night in [world]. */
    private fun sleepNeeded(world: World): Int {
        val eligible = world.players.count { it.gameMode != GameMode.SPECTATOR }
        val threshold = busybar.bedtimeHook?.threshold() ?: 1.0
        return max(1, ceil(eligible * threshold).toInt())
    }

    /** One-minute average TPS, rounded to one decimal. */
    private fun tps(): Double = roundTenths(busybar.server.tps[0])

    /** Average tick duration in milliseconds, rounded to one decimal. */
    private fun mspt(): Double = roundTenths(busybar.server.averageTickTime)

    /** Rounds [value] to one decimal, mapping NaN and infinities to 0 so they never reach the JSON payload. */
    private fun roundTenths(value: Double): Double =
        if (value.isFinite()) (value * 10).roundToLong() / 10.0 else 0.0

    companion object {
        /** Ticks between polls. */
        private const val POLL_TICKS = 20L

        /** Deviation from the expected time advance that counts as a jump. */
        private const val TIME_JUMP_TOLERANCE = 100L

        /** Seconds between `world.time` events when the clock did not jump. */
        private const val TIME_SYNC_SECONDS = 30
    }
}
