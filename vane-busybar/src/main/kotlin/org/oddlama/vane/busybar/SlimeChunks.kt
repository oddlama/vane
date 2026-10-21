package org.oddlama.vane.busybar

import java.util.*
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.json.JSONObject
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.ModuleComponent

/**
 * Tells a player's bridge when they stand in a slime chunk, but only while they carry a slime
 * bucket from vane-trifles: the same condition that makes the bucket's slime jump.
 *
 * Checked once per second for players with a connected bridge, so nobody else costs anything.
 *
 * @param context owning context.
 */
class SlimeChunks(context: Context<BusyBar?>) : ModuleComponent<BusyBar?>(context) {
    private val busybar: BusyBar
        get() = requireNotNull(module)

    /** Players last reported as in a slime chunk. */
    private val inside = mutableSetOf<UUID>()

    /** Repeating the check task. */
    private var task: BukkitTask? = null

    override fun onEnable() {
        task = scheduleTaskTimer({ poll() }, POLL_TICKS, POLL_TICKS)
    }

    override fun onDisable() {
        task?.cancel()
        task = null
        inside.clear()
    }

    /** Whether [player] is in a slime chunk and carries a working slime bucket. */
    fun active(player: Player): Boolean = player.chunk.isSlimeChunk && carriesSlimeBucket(player)

    private fun poll() {
        val online = mutableSetOf<UUID>()
        for (player in busybar.server.onlinePlayers) {
            val id = player.uniqueId
            online += id
            if (!busybar.hasBridge(id)) {
                inside -= id
                continue
            }
            val now = active(player)
            if (now == id in inside) continue
            if (now) inside += id else inside -= id
            busybar.stream.emit(
                if (now) "slimechunk.enter" else "slimechunk.leave",
                JSONObject()
                    .put("world", player.world.name)
                    .put("chunk_x", player.chunk.x)
                    .put("chunk_z", player.chunk.z),
                id,
                busybar.access.receiveSlimeChunk,
            )
        }
        inside.retainAll(online)
    }

    /**
     * Looks the bucket up in vane-core's item registry, so vane-trifles is not a compiler
     * dependency.
     */
    private fun carriesSlimeBucket(player: Player): Boolean {
        val registry = busybar.core?.itemRegistry() ?: return false
        val bucket = registry.get(SLIME_BUCKET)?.takeIf { it.enabled() } ?: return false
        return player.inventory.contents.any { it != null && registry.get(it) === bucket }
    }

    companion object {
        private const val POLL_TICKS = 20L

        /** vane-trifles' slime bucket. */
        private val SLIME_BUCKET = NamespacedKey("vane_trifles", "slime_bucket")
    }
}
