package org.oddlama.vane.bedtime

import java.util.*
import kotlin.math.ceil
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerBedLeaveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.oddlama.vane.annotation.VaneModule
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.util.Nms
import org.oddlama.vane.util.WorldUtil

@VaneModule(name = "bedtime", bstats = 8639, configVersion = 3, langVersion = 5, storageVersion = 1)
/**
 * Advances the night once enough players are sleeping and updates map integrations.
 *
 * @constructor Creates the bedtime module and registers its map integration components.
 */
class Bedtime : Module<Bedtime?>() {
    /** Tracks currently sleeping player UUIDs per world UUID. */
    private val worldSleepers = mutableMapOf<UUID, MutableSet<UUID>>()

    /** Fraction of non-spectator players that must be sleeping to trigger a night skip. */
    @ConfigDouble(
        def = 0.5,
        min = 0.0,
        max = 1.0,
        desc = "The percentage of sleeping players required to advance time.",
    )
    var configSleepThreshold: Double = 0.0

    /**
     * World time (in ticks) to advance to when the sleep threshold is met. 1000 is just after
     * sunrise.
     */
    @ConfigLong(
        def = 1000,
        min = 0,
        max = 12000,
        desc = "The target time in ticks to advance to. 1000 is just after sunrise.",
    )
    var configTargetTime: Long = 0

    /** Number of ticks over which the time transition is smoothly interpolated. */
    @ConfigLong(
        def = 100,
        min = 0,
        max = 1200,
        desc = "The interpolation time in ticks for a smooth change of time.",
    )
    var configInterpolationTicks: Long = 0

    /** Message broadcast to the world when a player enters a bed. */
    @LangMessage private val langPlayerBedEnter: TranslatedMessage? = null

    /** Message broadcast to the world when a player leaves a bed. */
    @LangMessage private val langPlayerBedLeave: TranslatedMessage? = null

    /** Dynmap integration component. */
    var dynmapLayer: BedtimeDynmapLayer = BedtimeDynmapLayer(this)

    /** BlueMap integration component. */
    var blueMapLayer: BedtimeBlueMapLayer = BedtimeBlueMapLayer(this)

    /** Schedules a delayed check for the sleep threshold in a world. */
    fun startCheckWorldTask(world: World) {
        if (enoughPlayersSleeping(world)) {
            scheduleTask({ checkWorldNow(world) }, 98L)
        }
    }

    /** Applies night skip effects immediately if the sleep threshold is still met. */
    fun checkWorldNow(world: World) {
        /* Abort task if condition changed. */
        if (!enoughPlayersSleeping(world)) {
            return
        }

        /* Let the sun rise, and clear weather. */
        WorldUtil.changeTimeSmoothly(world, this, configTargetTime, configInterpolationTicks)
        world.setStorm(false)
        world.isThundering = false

        /* Clear sleepers. */
        resetSleepers(world)

        /* Wake players as if they had slept through the night. */
        world.players.asSequence().filter(Player::isSleeping).forEach {
            /*
             * skipSleepTimer = false (sets sleepCounter to 100)
             * updateSleepingPlayers = false
             */
            Nms.getPlayer(it).stopSleepInBed(false, false)
        }
    }

    /** Registers a sleeping player, updates map markers, and schedules a sleep threshold check. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerBedEnter(event: PlayerBedEnterEvent) {
        val player = event.player
        val world = player.world

        /* Update map markers for this player. */
        dynmapLayer.updateMarker(player)
        blueMapLayer.updateMarker(player)

        scheduleNextTick {
            /* Only register the player if they actually went to sleep. Some plugins or
             * conditions may prevent entering the bed even though the interact event fired.
             * Checking Player.isSleeping here (on the next tick) ensures we only count
             * legitimate sleepers. */
            if (player.isSleeping) {
                /* Register the new player as sleeping. */
                addSleeping(world, player)
                /* Schedule a sleep threshold check. */
                startCheckWorldTask(world)
            }
        }
    }

    /** Removes the player from the sleeping set and broadcasts an updated progress message. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerBedLeave(event: PlayerBedLeaveEvent) {
        removeSleeping(event.player)
    }

    /** Re-evaluates the sleep threshold when a player disconnects, in case they were sleeping. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        /* Re-check sleeping threshold after a player leaves. */
        startCheckWorldTask(event.player.world)
    }

    /** Returns the number of players currently tracked as sleeping in [world]. */
    private fun getAmountSleeping(world: World): Long {
        return worldSleepers[world.uid]?.size?.toLong() ?: 0L
    }

    /** Returns the count of non-spectator players in [world] who are eligible to sleep. */
    private fun getPotentialSleepersInWorld(world: World): Long {
        return world.players.count { it.gameMode != GameMode.SPECTATOR }.toLong()
    }

    /**
     * Returns the ratio of sleeping players to potential sleepers in [world], or `0.0` if there are
     * no potential sleepers.
     */
    private fun getPercentageSleeping(world: World): Double {
        val countSleeping = getAmountSleeping(world)
        val potentialSleepers = getPotentialSleepersInWorld(world)
        if (countSleeping == 0L || potentialSleepers == 0L) {
            return 0.0
        }

        return countSleeping.toDouble() / potentialSleepers
    }

    /** Returns whether sleeping players in the world meet the configured threshold. */
    private fun enoughPlayersSleeping(world: World): Boolean {
        return getPercentageSleeping(world) >= configSleepThreshold
    }

    /**
     * Adds [player] to the sleeping set for [world] and broadcasts the updated progress message.
     */
    private fun addSleeping(world: World, player: Player) {
        val sleepers = worldSleepers.getOrPut(world.uid) { mutableSetOf() }

        sleepers.add(player.uniqueId)

        val percent = getPercentageSleeping(world)
        val amountSleeping = getAmountSleeping(world)
        val countRequired = ceil(getPotentialSleepersInWorld(world) * configSleepThreshold).toInt()
        broadcastSleepingMessage(
            langPlayerBedEnter,
            world,
            player,
            percent,
            amountSleeping,
            countRequired,
        )
    }

    /**
     * Removes [player] from the sleeping set and broadcasts the updated progress message, if the
     * player was actually registered as sleeping.
     */
    private fun removeSleeping(player: Player) {
        val world = player.world
        val sleepers = worldSleepers[world.uid] ?: return

        if (sleepers.remove(player.uniqueId)) {
            val percent = getPercentageSleeping(world)
            val countSleeping = getAmountSleeping(world)
            val countRequired =
                ceil(getPotentialSleepersInWorld(world) * configSleepThreshold).toInt()
            broadcastSleepingMessage(
                langPlayerBedLeave,
                world,
                player,
                percent,
                countSleeping,
                countRequired,
            )
        }
    }

    /** Clears all sleeping players tracked for [world]. */
    private fun resetSleepers(world: World) {
        val sleepers = worldSleepers[world.uid] ?: return

        sleepers.clear()
    }

    /** Utility functions for bedtime formatting. */
    companion object {
        /** Formats [percentage] (0.0–1.0) as a gold-coloured percentage string, e.g. `§650.00%`. */
        private fun percentageStr(percentage: Double): String =
            String.format("§6%.2f", 100.0 * percentage) + "%"
    }

    /** Broadcasts the shared action-bar sleep progress message for a world. */
    private fun broadcastSleepingMessage(
        message: TranslatedMessage?,
        world: World,
        player: Player,
        percent: Double,
        amountSleeping: Long,
        countRequired: Int,
    ) {
        requireNotNull(message)
            .broadcastWorldActionBar(
                world,
                "§6" + player.name,
                "§6" + percentageStr(percent),
                amountSleeping.toString(),
                countRequired.toString(),
                "§6" + world.name,
            )
    }
}
