package org.oddlama.vane.busybar.hooks

import kotlin.math.ceil
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import org.json.JSONObject
import org.oddlama.vane.admin.Admin
import org.oddlama.vane.admin.AutostopGroup
import org.oddlama.vane.busybar.BusyBar

/**
 * Streams vane-admin's autostop countdown and lets a bridge abort it.
 *
 * vane-admin has no autostop events, so the countdown is polled once per second.
 *
 * @param busybar owning module.
 * @param autostop vane-admin's autostop group.
 */
class AdminHook
private constructor(private val busybar: BusyBar, private val autostop: AutostopGroup) {
    /** Repeating the poll task. */
    private var task: BukkitTask? = null

    /** Remaining milliseconds seen at the previous poll, or -1 when nothing was scheduled. */
    private var lastRemaining = -1L

    /** Starts polling. */
    fun start() {
        lastRemaining = autostop.remaining()
        task = busybar.scheduleTaskTimer({ poll() }, POLL_TICKS, POLL_TICKS)
    }

    /** Stops polling. */
    fun stop() {
        task?.cancel()
        task = null
    }

    /** Seconds until the scheduled stop, or null when none is scheduled. */
    fun remainingSeconds(): Long? =
        autostop.remaining().takeIf { it >= 0 }?.let { ceil(it / 1000.0).toLong() }

    /** Aborts a scheduled stop on behalf of [by]. */
    fun abort(by: String) {
        if (autostop.remaining() < 0) return
        autostop.abort()
        lastRemaining = -1
        busybar.stream.emit(
            "autostop.aborted",
            JSONObject().put("reason", "manual").put("by", by),
            permission = busybar.access.receiveAutostop,
        )
    }

    /**
     * Emits `autostop.scheduled` for new or extended countdowns and `autostop.aborted` when one
     * ends early (a player joined or an admin ran `/autostop abort`).
     */
    private fun poll() {
        val remaining = autostop.remaining()
        val scheduled = remaining >= 0
        val wasScheduled = lastRemaining >= 0
        if (scheduled && (!wasScheduled || remaining > lastRemaining + RESCHEDULE_TOLERANCE_MS)) {
            busybar.stream.emit(
                "autostop.scheduled",
                JSONObject().put("remaining_s", remainingSeconds()),
                permission = busybar.access.receiveAutostop,
            )
        } else if (!scheduled && wasScheduled) {
            busybar.stream.emit(
                "autostop.aborted",
                JSONObject().put("reason", "cancelled"),
                permission = busybar.access.receiveAutostop,
            )
        }
        lastRemaining = remaining
    }

    companion object {
        /** Ticks between polls. */
        private const val POLL_TICKS = 20L

        /** A remaining time growing by more than this counts as a new schedule. */
        private const val RESCHEDULE_TOLERANCE_MS = 2000L

        /** Creates the hook when [plugin] is vane-admin. */
        fun create(busybar: BusyBar, plugin: Plugin): AdminHook? =
            (plugin as? Admin)?.let { AdminHook(busybar, it.autostopGroup) }
    }
}
