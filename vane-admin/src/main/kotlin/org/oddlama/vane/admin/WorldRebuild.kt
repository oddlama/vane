package org.oddlama.vane.admin

import kotlin.math.exp
import kotlin.math.max
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.scheduler.BukkitTask
import org.bukkit.util.Vector
import org.oddlama.vane.annotation.config.ConfigDouble
import org.oddlama.vane.annotation.config.ConfigLong
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.util.Nms
import org.oddlama.vane.util.msToTicks

/** Rebuilds exploded blocks over time instead of fully cancelling explosions. */
class WorldRebuild(context: Context<Admin?>) :
    Listener<Admin?>(
        context.group(
            "WorldRebuild",
            "Instead of cancelling explosions, the world will regenerate after a short amount of time.",
        )
    ) {
    private val admin: Admin
        get() = requireNotNull(module)

    @ConfigLong(
        def = 2000,
        min = 0,
        desc = "Delay in milliseconds until the world will be rebuilt.",
    )
    private val configDelay: Long = 0

    @ConfigDouble(
        def = 0.175,
        min = 0.0,
        desc =
            "Determines rebuild speed. Higher falloff means faster transition to quicker rebuild. After n blocks, the delay until the next block will be d_n = delay * exp(-x * delay_falloff). For example 0.0 will result in same delay for every block.",
    )
    private val configDelayFalloff = 0.0

    @ConfigLong(
        def = 50,
        min = 50,
        desc =
            "Minimum delay in milliseconds between rebuilding two blocks. Anything <= 50 milliseconds will be one tick.",
    )
    private val configMinDelay: Long = 0

    private val rebuilders = mutableListOf<Rebuilder>()

    /** Replaces exploded blocks with air and schedules asynchronous rebuild. */
    fun rebuild(blocks: MutableList<Block>) {
        val states = blocks.mapTo(mutableListOf()) { it.state }

        for (block in blocks) {
            Nms.setAirNoDrops(block)
        }

        rebuilders.add(Rebuilder(states))
    }

    /** Finishes all pending rebuilds immediately when the module is disabled. */
    public override fun onDisable() {
        for (r in rebuilders.toList()) {
            r.finishNow()
        }
        rebuilders.clear()
    }

    /** Rebuild task that restores captured block states one by one. */
    inner class Rebuilder(private var states: MutableList<BlockState>) : Runnable {
        private var task: BukkitTask? = null
        private var amountRebuild: Long = 0

        init {
            if (this.states.isNotEmpty()) {
                val center = Vector(0, 0, 0)
                var maxY = 0
                for (state in this.states) {
                    maxY = max(maxY, state.y)
                    center.add(state.location.toVector())
                }
                center.multiply(1.0 / this.states.size)
                center.setY(maxY + 1)

                this.states.sortWith(RebuildComparator(center))

                task = admin.scheduleTask(this, msToTicks(configDelay))
            }
        }

        /** Marks this rebuilder as complete and removes it from tracking. */
        private fun finish() {
            task = null
            this@WorldRebuild.rebuilders.remove(this)
        }

        /** Restores the next block state in rebuild order. */
        private fun rebuildNextBlock() {
            rebuildBlock(states.removeAt(states.size - 1))
        }

        /** Restores one captured block state and emits placement feedback. */
        private fun rebuildBlock(state: BlockState) {
            val block = state.block
            ++amountRebuild

            if (block.type != Material.AIR) {
                block.breakNaturally()
            }

            state.update(true, false)
            state.update(true, false)

            block.world.playSound(
                block.location,
                block.blockSoundGroup.placeSound,
                SoundCategory.BLOCKS,
                1.0f,
                0.8f,
            )
        }

        /** Restores all remaining states immediately and completes this rebuilder. */
        fun finishNow() {
            task?.cancel()

            for (state in states) {
                rebuildBlock(state)
            }

            finish()
        }

        /** Rebuilds the next block and schedules the following run with adaptive delay. */
        override fun run() {
            if (states.isEmpty()) {
                finish()
            } else {
                rebuildNextBlock()

                val delay =
                    msToTicks(
                        max(
                            configMinDelay,
                            (configDelay * exp(-amountRebuild * configDelayFalloff))
                                .toInt()
                                .toLong(),
                        )
                    )
                task = admin.scheduleTask(this, delay)
            }
        }
    }

    /** Orders blocks by distance to a top-center reference point. */
    class RebuildComparator(private val referencePoint: Vector) : Comparator<BlockState> {
        /** Compares two block states by squared distance to the reference point. */
        override fun compare(a: BlockState, b: BlockState): Int {
            val da = a.location.toVector().subtract(referencePoint).lengthSquared()
            val db = b.location.toVector().subtract(referencePoint).lengthSquared()
            return compareValues(da, db)
        }
    }
}
