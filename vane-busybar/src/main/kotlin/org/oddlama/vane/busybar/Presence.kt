package org.oddlama.vane.busybar

import java.util.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.json.JSONObject
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

/**
 * Tracks which players have their BUSY Bar in focus mode and tags them in the tab list.
 *
 * The state survives restarts and players going offline, so a bar left in focus mode keeps its tag
 * when its owner rejoins.
 *
 * @param context owning context.
 */
class Presence(context: Context<BusyBar?>) : Listener<BusyBar?>(context) {
    private val busybar: BusyBar
        get() = requireNotNull(module)

    /** Appended to the tab list name of busy players. */
    @LangMessage var langTabTag: TranslatedMessage? = null

    /** Players whose bar is in focus mode. */
    @Persistent var storageBusy: MutableSet<UUID?> = mutableSetOf()

    /** Players in focus mode, online or not. */
    fun busyPlayers(): List<UUID> = storageBusy.filterNotNull()

    /** Whether [player] is in focus mode. */
    fun isBusy(player: UUID): Boolean = player in storageBusy

    /** Updates the focus state of [player] and confirms it to their bridges. */
    fun setBusy(player: UUID, busy: Boolean) {
        val changed = if (busy) storageBusy.add(player) else storageBusy.remove(player)
        if (changed) markPersistentStorageDirty()
        busybar.server.getPlayer(player)?.let(::applyTag)
        busybar.stream.emit("presence.updated", JSONObject().put("busy", busy), player)
    }

    /** Re-applies the tag one tick after joining, so join messages use the plain name. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (isBusy(player.uniqueId)) scheduleNextTick { if (player.isOnline) applyTag(player) }
    }

    /**
     * Drops the tag before anything builds a leave message: vane-admin's quit and kick messages use
     * the tab list name. Focus mode stays stored, so the tag returns on the next join.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (isBusy(event.player.uniqueId)) event.player.playerListName(null)
    }

    /** Sets or clears the tab list tag of [player]. */
    private fun applyTag(player: Player) {
        player.playerListName(
            if (isBusy(player.uniqueId)) player.displayName().append(langTabTag!!.format())
            else null
        )
    }
}
