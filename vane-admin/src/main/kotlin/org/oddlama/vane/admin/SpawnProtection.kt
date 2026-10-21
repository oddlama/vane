package org.oddlama.vane.admin

import kotlin.math.sqrt
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.*
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionDefault
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.annotation.config.ConfigInt
import org.oddlama.vane.annotation.config.ConfigString
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context

/** Protects a configurable spawn area from block and entity interactions. */
class SpawnProtection(context: Context<Admin?>) :
    Listener<Admin?>(
        context.groupDefaultDisabled(
            "SpawnProtection",
            "Enable spawn protection. Slightly more sophisticated than the vanilla spawn protection, if you need even more control, use regions. This will prevent anyone from modifying the spawn of the world if they don't have the permission '" +
                PERMISSION_NAME +
                "'.",
        )
    ) {
    private val admin: Admin
        get() = requireNotNull(module)

    private val permission =
        Permission(
            PERMISSION_NAME,
            "Allow player to bypass spawn protection",
            PermissionDefault.OP,
        )

    @ConfigBoolean(def = true, desc = "Allow interaction events at spawn (buttons, levers, etc.).")
    private val configAllowInteraction = false

    @ConfigInt(def = 64, min = 0, desc = "Radius to protect.") private val configRadius = 0

    @ConfigString(def = "world", desc = "The spawn world.") private val configWorld: String? = null

    @ConfigBoolean(
        def = true,
        desc = "Use world's spawn location instead of the specified center coordinates.",
    )
    private val configUseSpawnLocation = false

    @ConfigInt(def = 0, desc = "Center X coordinate.") private val configX = 0

    @ConfigInt(def = 0, desc = "Center Z coordinate.") private val configZ = 0

    private var spawnCenter: Location? = null

    init {
        admin.registerPermission(permission)
    }

    /** Recomputes the protected spawn center after config updates. */
    public override fun onConfigChange() {
        spawnCenter = null
        scheduleNextTick {
            val worldName = configWorld ?: return@scheduleNextTick
            val world: World? = admin.server.getWorld(worldName)
            if (world == null) {
                /** TODO print error and show valid worlds. */
                admin.log.warning(
                    "The world \"$configWorld\" configured for spawn-protection could not be found."
                )
                admin.log.warning("These are the names of worlds existing on this server:")
                for (w in admin.server.worlds) {
                    admin.log.warning("  \"${w.name}\"")
                }
                spawnCenter = null
            } else {
                spawnCenter =
                    if (configUseSpawnLocation) {
                        world.spawnLocation.apply { y = 0.0 }
                    } else {
                        Location(world, configX.toDouble(), 0.0, configZ.toDouble())
                    }
            }
        }
    }

    /** Convenience overload for block-based checks. */
    fun denyModifySpawn(block: Block, entity: Entity?): Boolean {
        return denyModifySpawn(block.location, entity)
    }

    /** Returns true when a player attempts to modify a protected spawn location. */
    fun denyModifySpawn(location: Location, entity: Entity?): Boolean {
        val center = spawnCenter ?: return false
        val player = entity as? Player ?: return false

        val dx = location.x - center.x
        val dz = location.z - center.z
        val distance = sqrt(dx * dx + dz * dz)
        return distance <= configRadius && !player.hasPermission(permission)
    }

    /** Cancels block breaking in protected spawn area. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (denyModifySpawn(event.block, event.player)) {
            event.isCancelled = true
        }
    }

    /** Cancels block placement in protected spawn area. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (denyModifySpawn(event.block, event.player)) {
            event.isCancelled = true
        }
    }

    /** Cancels hanging-entity break actions in protected spawn area. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onHangingBreakByEntity(event: HangingBreakByEntityEvent) {
        if (denyModifySpawn(event.entity.location, event.remover)) {
            event.isCancelled = true
        }
    }

    /** Cancels hanging-entity placement in protected spawn area. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onHangingPlace(event: HangingPlaceEvent) {
        if (denyModifySpawn(event.entity.location, event.player)) {
            event.isCancelled = true
        }
    }

    /** Cancels armor-stand manipulation in protected spawn area. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerArmorStandManipulate(event: PlayerArmorStandManipulateEvent) {
        if (denyModifySpawn(event.rightClicked.location, event.player)) {
            event.isCancelled = true
        }
    }

    /** Cancels bucket emptying in protected spawn area. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketEmpty(event: PlayerBucketEmptyEvent) {
        if (denyModifySpawn(event.block, event.player)) {
            event.isCancelled = true
        }
    }

    /** Cancels bucket filling in protected spawn area. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerBucketFill(event: PlayerBucketFillEvent) {
        if (denyModifySpawn(event.block, event.player)) {
            event.isCancelled = true
        }
    }

    /** Cancels entity interaction in protected spawn area when interactions are disabled. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        if (!configAllowInteraction && denyModifySpawn(event.rightClicked.location, event.player)) {
            event.isCancelled = true
        }
    }

    /** Cancels block interaction in protected spawn area when interactions are disabled. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val clickedBlock = event.clickedBlock ?: return
        if (!configAllowInteraction && denyModifySpawn(clickedBlock, event.player)) {
            event.isCancelled = true
        }
    }

    companion object {
        private const val PERMISSION_NAME = "vane.admin.bypass_spawn_protection"
    }
}
