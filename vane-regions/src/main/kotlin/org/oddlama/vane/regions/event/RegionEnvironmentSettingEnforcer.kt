package org.oddlama.vane.regions.event

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.*
import org.bukkit.event.hanging.HangingBreakEvent
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause
import org.bukkit.event.player.PlayerInteractEvent
import org.oddlama.vane.core.Listener
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.regions.Regions
import org.oddlama.vane.regions.region.EnvironmentSetting

/** Enforces environment-related region settings such as PVP, fire, and mob spawning. */
class RegionEnvironmentSettingEnforcer(context: Context<Regions?>?) : Listener<Regions?>(context) {
    /** Owning regions module instance. */
    private val regions: Regions
        get() = requireNotNull(module)

    /** Checks whether an environment setting at a location matches the expected value. */
    fun checkSettingAt(
        location: Location,
        setting: EnvironmentSetting,
        checkAgainst: Boolean,
    ): Boolean {
        /** Region at the queried location. */
        val region = regions.regionAt(location) ?: return false

        /** Region group owning that region. */
        val group = region.regionGroup(regions) ?: return false
        return group.getSetting(setting) == checkAgainst
    }

    /** Checks whether an environment setting at a block matches the expected value. */
    fun checkSettingAt(block: Block, setting: EnvironmentSetting, checkAgainst: Boolean): Boolean {
        val region = regions.regionAt(block) ?: return false
        val group = region.regionGroup(regions) ?: return false
        return group.getSetting(setting) == checkAgainst
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Prevents explosion block damage where `EXPLOSIONS` is denied. */
    fun onBlockExplode(event: BlockExplodeEvent) {
        // Prevent explosions from removing region blocks
        event.blockList().removeIf { block ->
            checkSettingAt(block, EnvironmentSetting.EXPLOSIONS, false)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Prevents entity-caused explosion block damage where `EXPLOSIONS` is denied. */
    fun onEntityExplode(event: EntityExplodeEvent) {
        // Prevent explosions from removing region blocks
        event.blockList().removeIf { block ->
            checkSettingAt(block, EnvironmentSetting.EXPLOSIONS, false)
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Prevents monster block changes where monster interactions are denied. */
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (event.entity !is Monster) {
            return
        }

        // Prevent monster entities from changing region blocks
        if (checkSettingAt(event.block, EnvironmentSetting.MONSTERS, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels burning where `FIRE` is denied. */
    fun onBlockBurn(event: BlockBurnEvent) {
        if (checkSettingAt(event.block, EnvironmentSetting.FIRE, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Cancels fire/vine spread where corresponding settings are denied. */
    fun onBlockSpread(event: BlockSpreadEvent) {
        val setting =
            when (event.newState.type) {
                Material.FIRE -> EnvironmentSetting.FIRE
                Material.VINE -> EnvironmentSetting.VINE_GROWTH
                else -> return
            }

        if (checkSettingAt(event.block, setting, false)) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Restricts natural animal/monster spawns based on region settings. */
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        // Only cancel natural spawns and alike
        when (event.spawnReason) {
            CreatureSpawnEvent.SpawnReason.JOCKEY,
            CreatureSpawnEvent.SpawnReason.MOUNT,
            CreatureSpawnEvent.SpawnReason.NATURAL -> {}
            else -> return
        }

        val entity = event.entity
        if (entity is Monster) {
            if (checkSettingAt(event.location, EnvironmentSetting.MONSTERS, false)) {
                event.isCancelled = true
            }
        } else if (entity is Animals) {
            if (checkSettingAt(event.location, EnvironmentSetting.ANIMALS, false)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Enforces PVP restrictions for direct and projectile player damage. */
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damaged = event.getEntity()
        val damager = event.damager

        if (damaged.type != EntityType.PLAYER) {
            return
        }

        val playerDamaged = damaged as Player
        val playerDamager =
            when (damager) {
                is Player -> {
                    damager
                }

                is Projectile if damager.shooter is Player -> {
                    damager.shooter as Player?
                }

                else -> {
                    return
                }
            }

        if (
            playerDamager != null &&
                playerDamaged !== playerDamager &&
                (checkSettingAt(playerDamaged.location, EnvironmentSetting.PVP, false) ||
                    checkSettingAt(playerDamager.location, EnvironmentSetting.PVP, false))
        ) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Prevents explosion-based hanging breaks where explosions are denied. */
    fun onHangingBreakEvent(event: HangingBreakEvent) {
        when (event.cause) {
            RemoveCause.ENTITY -> return // Handeled by onHangingBreakByEntity
            RemoveCause.EXPLOSION -> {
                if (checkSettingAt(event.entity.location, EnvironmentSetting.EXPLOSIONS, false)) {
                    event.isCancelled = true
                }
            }

            else -> return
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Prevents farmland trampling when `TRAMPLE` is denied. */
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.PHYSICAL) {
            return
        }

        val block = event.clickedBlock
        if (block != null && block.type == Material.FARMLAND) {
            if (checkSettingAt(block, EnvironmentSetting.TRAMPLE, false)) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    /** Enforces PVP restrictions for splash-potion effects. */
    fun onPotionSplash(event: PotionSplashEvent) {
        // Only if a player threw the potion check for PVP
        val thrower = event.entity.shooter as? Player ?: return
        val sourcePvpRestricted = checkSettingAt(thrower.location, EnvironmentSetting.PVP, false)

        // Cancel all damage to players if either thrower or damaged is
        // inside no-PVP region
        for (target in event.affectedEntities) {
            if (target !is Player) {
                continue
            }

            if (
                sourcePvpRestricted ||
                    checkSettingAt(target.location, EnvironmentSetting.PVP, false)
            ) {
                event.setIntensity(target, 0.0)
                return
            }
        }
    }
}
