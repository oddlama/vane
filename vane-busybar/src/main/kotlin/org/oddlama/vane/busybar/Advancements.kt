package org.oddlama.vane.busybar

import io.papermc.paper.advancement.AdvancementDisplay
import io.papermc.paper.adventure.PaperAdventure
import java.util.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.json.JSONObject
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

/**
 * Forwards advancements that the game announces in chat, so recipes and hidden ones stay quiet.
 *
 * A player's advancements are collected for one second and sent as one event with a count:
 * `/advancement grant ... everything` completes hundreds in a single tick, which would otherwise
 * overrun every bridge's queue.
 *
 * @param context owning context.
 */
class Advancements(context: Context<BusyBar?>) : Listener<BusyBar?>(context) {
    private val busybar: BusyBar
        get() = requireNotNull(module)

    /** Advancements of one player waiting to be sent. */
    private class Batch(val player: String, val head: String?) {
        var count = 0
        var title = ""
        var frame = AdvancementDisplay.Frame.TASK
    }

    /** Pending batches per player. */
    private val batches = mutableMapOf<UUID, Batch>()

    override fun onDisable() {
        batches.clear()
        super.onDisable()
    }

    /** Adds the advancement to the player's batch, starting the one-second window if needed. */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onAdvancementDone(event: PlayerAdvancementDoneEvent) {
        val display = event.advancement.display ?: return
        if (!display.doesAnnounceToChat()) return
        val player = event.player
        val batch =
            batches.getOrPut(player.uniqueId) {
                scheduleTask({ flush(player.uniqueId) }, BATCH_TICKS)
                Batch(player.name, busybar.headOf(player))
            }
        batch.count++
        // Show the rarest one: challenges outrank goals, which outrank tasks.
        if (batch.count == 1 || rank(display.frame()) >= rank(batch.frame)) {
            batch.frame = display.frame()
            batch.title = PaperAdventure.asPlain(display.title(), Locale.US)
        }
    }

    private fun flush(player: UUID) {
        val batch = batches.remove(player) ?: return
        busybar.stream.emit(
            "advancement",
            JSONObject()
                .put("player", batch.player)
                .put("head", batch.head ?: JSONObject.NULL)
                .put("title", batch.title)
                .put("frame", batch.frame.name.lowercase())
                .put("count", batch.count),
            permission = busybar.access.receiveAdvancements,
        )
    }

    companion object {
        /** Ticks a batch stays open. */
        private const val BATCH_TICKS = 20L

        /** Rarity of a frame. Explicit, because the enum declares CHALLENGE first. */
        private fun rank(frame: AdvancementDisplay.Frame): Int =
            when (frame) {
                AdvancementDisplay.Frame.TASK -> 0
                AdvancementDisplay.Frame.GOAL -> 1
                AdvancementDisplay.Frame.CHALLENGE -> 2
            }
    }
}
