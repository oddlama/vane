package org.oddlama.vane.core.misc

import java.util.*
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.loot.Lootable
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.lang.TranslatedMessage
import org.oddlama.vane.core.module.Context

/**
 * Adds a two-step confirmation window before breaking loot-table blocks.
 *
 * @param context listener context.
 */
class LootChestProtector(context: Context<Core?>?) : Listener<Core?>(context) {
    /** First break attempts by block and player UUID with timestamp. */
    private val lootBreakAttempts: MutableMap<Block, MutableMap<UUID, Long>> = mutableMapOf()

    /** Whether loot-table block break confirmation is enabled. */
    @ConfigBoolean(
        def = true,
        desc =
            "Prevent players from breaking blocks with loot-tables (like treasure chests) when they first attempt to destroy it. They still can break it, but must do so within a short timeframe.",
    )
    /** Whether loot-table block break protection warning is enabled. */
    var configWarnBreakingLootBlocks: Boolean = false

    /** Message sent when initial break attempt is blocked. */
    @LangMessage var langBreakLootBlockPrevented: TranslatedMessage? = null

    /** Handles loot-table block break confirmation timing logic. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBreakLootChest(event: BlockBreakEvent) {
        if (!configWarnBreakingLootBlocks) return

        val lootable = event.block.getState(false) as? Lootable ?: return
        if (!lootable.hasLootTable()) return

        val block = event.block
        val player = event.player
        val now = System.currentTimeMillis()

        val blockAttempts = lootBreakAttempts.getOrPut(block) { mutableMapOf() }
        val previousAttempt = blockAttempts[player.uniqueId]

        if (previousAttempt != null) {
            val elapsed = now - previousAttempt
            if (elapsed in 5001L until 30000L) return
        }

        blockAttempts[player.uniqueId] = now
        langBreakLootBlockPrevented!!.send(player)
        event.isCancelled = true
    }
}
