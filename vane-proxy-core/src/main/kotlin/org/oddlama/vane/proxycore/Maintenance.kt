package org.oddlama.vane.proxycore

import java.io.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import org.oddlama.vane.proxycore.scheduler.ProxyScheduledTask
import org.oddlama.vane.proxycore.util.formatTime

@Suppress("unused")
/**
 * Coordinates maintenance mode scheduling, persistence, and player notifications.
 *
 * @property plugin owning proxy plugin.
 */
class Maintenance(private val plugin: VaneProxyPlugin) {
    /** Persistence file for maintenance state across restarts. */
    private val file = File("./.maintenance")

    /** Task responsible for enabling maintenance at the scheduled time. */
    private val taskEnable = TaskEnable()

    /** Task responsible for periodic countdown notifications. */
    private val taskNotify = TaskNotify()

    /** Current maintenance activation state. */
    private var enabled = false

    /** Scheduled start timestamp in epoch milliseconds. */
    private var start: Long = 0

    /** Scheduled maintenance duration in milliseconds, or `null` for indefinite duration. */
    private var duration: Long? = 0L

    /**
     * Returns the scheduled maintenance start time.
     *
     * @return epoch milliseconds.
     */
    fun start(): Long = start

    /**
     * Returns the configured maintenance duration.
     *
     * @return duration in milliseconds or `null` for indefinite.
     */
    fun duration(): Long? = duration

    /**
     * Indicates whether maintenance mode is currently active.
     *
     * @return `true` when maintenance mode is enabled.
     */
    fun enabled(): Boolean = enabled

    /** Enables maintenance immediately and disconnects all connected players. */
    fun enable() {
        enabled = true

        // Kick all players
        val kickMessage = formatMessage(MESSAGE_KICK)
        plugin.proxy.players?.forEach { player ->
            player?.disconnect(kickMessage)
        }

        plugin.getLogger().log(Level.INFO, "Maintenance enabled!")
    }

    /** Resets maintenance state and cancels all scheduled maintenance tasks. */
    fun disable() {
        start = 0
        duration = 0L
        enabled = false

        taskEnable.cancel()
        taskNotify.cancel()

        // Delete file
        file.delete()
    }

    /** Aborts scheduled or active maintenance and emits cancellation broadcasts when applicable. */
    fun abort() {
        if (start == 0L) {
            return
        }

        if (start - System.currentTimeMillis() > 0) {
            // Broadcast message (only if not started yet)
            plugin.proxy.broadcast(MESSAGE_ABORTED)
        }

        // Disable maintenance (just to be on the safe side)
        disable()

        plugin.getLogger().log(Level.INFO, "Maintenance disabled!")
    }

    /**
     * Schedules maintenance mode activation.
     *
     * @param startMillis activation timestamp in epoch milliseconds.
     * @param durationMillis duration in milliseconds, or `null` for indefinite duration.
     */
    fun schedule(startMillis: Long, durationMillis: Long?) {
        if (durationMillis == null && enabled) {
            plugin.getLogger().log(Level.WARNING, "Maintenance already enabled!")
            return
        }

        // Schedule maintenance
        enabled = false
        start = startMillis
        duration = durationMillis

        // Save to file
        save()

        // Start tasks
        taskEnable.schedule()

        if (durationMillis != null) {
            taskNotify.schedule()
        }
    }

    /** Loads persisted maintenance state from disk and restores scheduling or active mode. */
    fun load() {
        if (file.exists()) {
            // Recover maintenance times

            try {
                FileReader(file).use { fileReader ->
                    BufferedReader(fileReader).use { reader ->
                        start = reader.readLine().toLong()
                        val durationLine = reader.readLine()
                        if (durationLine != null) {
                            duration = durationLine.toLong()
                        } else {
                            // We have no duration, run until stopped
                            duration = null
                            enable()
                            return
                        }
                    }
                }
            } catch (e: IOException) {
                plugin.getLogger().log(Level.WARNING, "Failed to read maintenance file", e)
                disable()
                return
            } catch (e: NumberFormatException) {
                plugin.getLogger().log(Level.WARNING, "Invalid maintenance file contents", e)
                disable()
                return
            }

            val delta = System.currentTimeMillis() - start
            if (delta < 0) {
                // Maintenance scheduled but not active
                schedule(start, duration)
            } else if (duration?.let { delta - it < 0 } == true) {
                // Maintenance still active
                enable()
            } else {
                // Maintenance already over
                disable()
            }
        } else {
            disable()
        }
    }

    /** Persists the current maintenance schedule to disk. */
    fun save() {
        // create and write file
        try {
            FileWriter(file).use { writer ->
                if (duration != null) {
                    writer.write("$start\n$duration")
                } else {
                    writer.write("$start")
                }
            }
        } catch (e: IOException) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save maintenance state to file", e)
        }
    }

    /**
     * Expands placeholders in a maintenance message template.
     *
     * @param message template containing `%MOTD%`, `%time%`, `%duration%`, and `%remaining%`
     *   placeholders.
     * @return formatted message.
     */
    fun formatMessage(message: String): String {
        var timespan = start - System.currentTimeMillis()
        val time: String

        if (timespan <= 0) {
            time = "Now"
        } else {
            if (timespan % 1000 >= 500) {
                timespan += 1000
            }
            time = formatTime(timespan)
        }

        val durationString: String
        val remainingString: String
        val currentDuration = duration
        if (currentDuration != null) {
            var remaining = currentDuration + (start - System.currentTimeMillis())
            if (remaining > currentDuration) {
                remaining = currentDuration
            } else if (remaining < 0) {
                remaining = 0
            }

            durationString = formatTime(currentDuration)
            remainingString = formatTime(remaining)
        } else {
            durationString = "Indefinite"
            remainingString = "Indefinite"
        }

        return message
            .replace("%MOTD%", MOTD)
            .replace("%time%", time)
            .replace("%duration%", durationString)
            .replace("%remaining%", remainingString)
    }

    /** Task that broadcasts periodic countdown updates before maintenance starts. */
    inner class TaskNotify : Runnable {
        /** Currently scheduled proxy task, if any. */
        private var task: ProxyScheduledTask? = null

        /** Next countdown checkpoint in milliseconds before start. */
        private var notifyTime: Long = -1

        /** Runs the notification task and schedules the next checkpoint. */
        @Synchronized
        override fun run() {
            // Broadcast message
            plugin.proxy.broadcast(
                formatMessage(
                    if (notifyTime <= SHUTDOWN_THRESHOLD) MESSAGE_SHUTDOWN else MESSAGE_SCHEDULED
                )
            )

            // Schedule next time
            schedule()
        }

        /** Cancels the current scheduled notification task and resets state. */
        @Synchronized
        fun cancel() {
            task?.cancel()
            task = null
            notifyTime = -1
        }

        /** Computes and schedules the next notification checkpoint. */
        @Synchronized
        fun schedule() {
            // cancel if running
            cancel()

            // subtract 500 millis, so we will never "forget" one step
            val timespan = start - System.currentTimeMillis() - 500

            if (notifyTime < 0) {
                // First schedule
                plugin.proxy.broadcast(formatMessage(MESSAGE_SCHEDULED))
                notifyTime = timespan
            }

            if ((nextNotifyTime().also { notifyTime = it }) < 0) {
                // No next time
                return
            }

            // Schedule for next time
            val scheduler = plugin.proxy.scheduler
            if (scheduler == null) {
                plugin
                    .getLogger()
                    .log(
                        Level.SEVERE,
                        "No scheduler available to schedule maintenance notifications",
                    )
                return
            }

            task = scheduler.schedule(plugin, this, timespan - notifyTime, TimeUnit.MILLISECONDS)
        }

        /**
         * Resolves the next countdown checkpoint below the current [notifyTime].
         *
         * @return next checkpoint in milliseconds, or `-1` when no further notification is needed.
         */
        fun nextNotifyTime(): Long {
            if (notifyTime < 0) {
                return -1
            }

            for (t in NOTIFY_TIMES) {
                if (notifyTime - t > 0) {
                    return t
                }
            }

            return -1
        }
    }

    /** Task that enables maintenance mode once the scheduled start time is reached. */
    inner class TaskEnable : Runnable {
        /** Currently scheduled enable task, if any. */
        private var task: ProxyScheduledTask? = null

        /** Executes maintenance activation. */
        @Synchronized
        override fun run() {
            this@Maintenance.enable()
            task = null
        }

        /** Cancels the current scheduled activation task. */
        @Synchronized
        fun cancel() {
            task?.cancel()
            task = null
        }

        /** Schedules activation at the configured start timestamp. */
        @Synchronized
        fun schedule() {
            // Cancel if running
            cancel()

            // New task
            var timespan = this@Maintenance.start() - System.currentTimeMillis()
            if (timespan < 0) {
                timespan = 0
            }

            val scheduler = plugin.proxy.scheduler
            if (scheduler == null) {
                plugin
                    .getLogger()
                    .log(Level.SEVERE, "No scheduler available to schedule maintenance enable task")
                return
            }

            task = scheduler.schedule(plugin, this, timespan, TimeUnit.MILLISECONDS)
        }
    }

    companion object {
        /** Countdown points (in milliseconds) at which notifications are emitted. */
        val NOTIFY_TIMES: LongArray =
            longArrayOf(
                240 * 60000L,
                180 * 60000L,
                120 * 60000L,
                60 * 60000L,
                30 * 60000L,
                15 * 60000L,
                10 * 60000L,
                5 * 60000L,
                4 * 60000L,
                3 * 60000L,
                2 * 60000L,
                60000L,
                30000L,
                10000L,
                5000L,
                4000L,
                3000L,
                2000L,
                1000L,
            )

        /** Threshold in milliseconds for switching to the shutdown warning message. */
        var SHUTDOWN_THRESHOLD: Long = 10000L // MESSAGE_SHUTDOWN if <= 10 seconds

        /** Broadcast template used when maintenance is cancelled. */
        var MESSAGE_ABORTED: String = "§7> §cServer maintenance §l§6CANCELLED§r§c!"

        /** Informational template used by maintenance status queries. */
        var MESSAGE_INFO: String =
            "§7>" +
                "\n§7> §cScheduled maintenance in: §6%time%" +
                "\n§7> §cExpected time remaining: §6%remaining%" +
                "\n§7>"

        /** Broadcast template used while maintenance is scheduled but not yet active. */
        var MESSAGE_SCHEDULED: String =
            "§7>" +
                "\n§7> §e\u21af§r §6§lMaintenance active§r §e\u21af§r" +
                "\n§7>" +
                "\n§7> §cScheduled maintenance in: §6%time%" +
                "\n§7> §cExpected duration: §6%duration%" +
                "\n§7>"

        /** Broadcast template used near activation time. */
        var MESSAGE_SHUTDOWN: String = "§7> §cShutdown in §6%time%§c!"

        /** Disconnect message sent to players when maintenance activates. */
        var MESSAGE_KICK: String =
            "§e\u21af§r §6§lMaintenance active§r §e\u21af§r" + "\n§cExpected duration: §6%duration%"

        /** MOTD template exposed during active maintenance. */
        @JvmField
        var MOTD: String =
            "§e\u21af§r §6§lMaintenance active§r §e\u21af§r" +
                "\n§cExpected time remaining: §6%remaining%"

        /** Message shown when users attempt to connect during maintenance. */
        var MESSAGE_CONNECT: String = "%MOTD%" + "\n" + "\n§7Please try again later."
    }
}
