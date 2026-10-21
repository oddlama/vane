package org.oddlama.vane.admin

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.oddlama.vane.core.Listener

/** Starts and aborts autostop scheduling based on player join/leave events. */
class AutostopListener(private val autostop: AutostopGroup) : Listener<Admin?>(autostop) {
    private val admin: Admin
        get() = requireNotNull(module)

    /** Aborts any scheduled shutdown when a player joins. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerJoin(@Suppress("UNUSED_PARAMETER") event: PlayerJoinEvent) {
        autostop.abort()
    }

    /** Re-evaluates autostop scheduling when a player is kicked. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerKick(event: PlayerKickEvent) {
        playerLeave(event.player)
    }

    /** Re-evaluates autostop scheduling when a player quits. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        playerLeave(event.player)
    }

    /** Schedules autostop when the leaving player was the last online player. */
    private fun playerLeave(player: Player) {
        val players = admin.server.onlinePlayers
        if (players.none { it != player }) {
            autostop.schedule()
        }
    }
}
