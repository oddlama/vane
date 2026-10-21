package org.oddlama.vane.util

import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/** World-related utility helpers. */
object WorldUtil {
    /** Running smooth-time tasks keyed by world UUID. */
    private val runningTimeChangeTasks = mutableMapOf<UUID, BukkitTask>()

    /**
     * Smoothly interpolates world time to a target tick value.
     *
     * @return false when a smooth-time task is already running for the world.
     */
    @JvmStatic
    fun changeTimeSmoothly(
        world: World,
        plugin: Plugin,
        worldTicks: Long,
        interpolationTicks: Long,
    ): Boolean {
        synchronized(runningTimeChangeTasks) {
            if (world.uid in runningTimeChangeTasks) return false

            val relFrom = world.time
            val relTo = if (worldTicks > relFrom) worldTicks else worldTicks + 24000
            val deltaTicks = relTo - relFrom

            var elapsed = 0L
            val task =
                plugin.server.scheduler.runTaskTimer(
                    plugin,
                    Runnable {
                        if (elapsed > interpolationTicks) {
                            synchronized(runningTimeChangeTasks) {
                                runningTimeChangeTasks.remove(world.uid)!!.cancel()
                            }
                        }
                        val linDelta = elapsed.toFloat() / interpolationTicks
                        val delta = (1f - cos(PI * linDelta).toFloat()) / 2f
                        // Use relative day time to avoid deprecated fullTime setter.
                        world.time = (relFrom + (deltaTicks * delta).toLong()) % 24000L
                        elapsed++
                    },
                    1L,
                    1L,
                )

            runningTimeChangeTasks[world.uid] = task
        }
        return true
    }
}
