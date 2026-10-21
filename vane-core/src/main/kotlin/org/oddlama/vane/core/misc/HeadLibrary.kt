package org.oddlama.vane.core.misc

import java.io.IOException
import java.util.logging.Level
import org.bukkit.Material
import org.bukkit.block.Skull
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.core.Core
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.material.HeadMaterialLibrary.fromTexture
import org.oddlama.vane.core.material.HeadMaterialLibrary.load
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.BlockUtil.dropNaturally
import org.oddlama.vane.util.BlockUtil.textureFromSkull

/**
 * Loads the bundled head library and optionally normalizes dropped player heads.
 *
 * @param context listener context.
 */
class HeadLibrary(context: Context<Core?>?) : Listener<Core?>(context) {
    /** Whether matching player-head block drops should be replaced with library items. */
    @ConfigBoolean(
        def = true,
        desc =
            "When a player head is broken by a player that exists in /heads, drop the correctly named item as seen in /heads. You can disable this if it interferes with similarly textured heads from other plugins.",
    )
    /** Whether matching player-head drops should be replaced with head-library entries. */
    var configPlayerHeadDrops: Boolean = false

    init {
        module!!.log.info("Loading head library...")
        try {
            val json =
                module!!.getResource("head_library.json")?.bufferedReader()?.readText()
                    ?: throw IOException("Failed to get contents of resource head_library.json")
            load(json)
        } catch (e: IOException) {
            module!!
                .log
                .log(Level.SEVERE, "Error while loading head_library.json! Shutting down.", e)
            module!!.server.shutdown()
        }
    }

    /** Replaces broken player-head block drops with matching head-library items. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!configPlayerHeadDrops) return

        val block = event.block
        if (block.type != Material.PLAYER_HEAD && block.type != Material.PLAYER_WALL_HEAD) return

        val texture = textureFromSkull(block.state as? Skull ?: return) ?: return
        val headMaterial = fromTexture(texture) ?: return

        block.type = Material.AIR
        dropNaturally(block, headMaterial.item())
    }
}
