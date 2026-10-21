package org.oddlama.vane.trifles

import io.papermc.paper.event.entity.EntityMoveEvent
import net.minecraft.world.entity.monster.Monster
import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerMoveEvent
import org.oddlama.vane.annotation.config.ConfigBoolean
import org.oddlama.vane.core.Listener

/** Applies configured speed effects to players and optionally other moving entities. */
class FastWalkingListener(private val fastWalking: FastWalkingGroup) :
    Listener<Trifles?>(fastWalking) {
    /** Whether hostile mobs are eligible for path speed effects. */
    @ConfigBoolean(
        def = false,
        desc = "Whether hostile mobs should be allowed to fast walk on paths.",
    )
    var configHostileSpeedwalk: Boolean = false

    /** Whether villagers are eligible for path speed effects. */
    @ConfigBoolean(def = true, desc = "Whether villagers should be allowed to fast walk on paths.")
    var configVillagerSpeedwalk: Boolean = false

    /** Whether non-player entities should always be excluded from speedwalking. */
    @ConfigBoolean(
        def = false,
        desc =
            "Whether players should be the only entities allowed to fast walk on paths (will override other path walk settings).",
    )
    /** Whether non-player entities should always be excluded from speedwalking. */
    var configPlayersOnlySpeedwalk: Boolean = false

    /** Applies the fast-walk effect to players (or their ridden living entity). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Gliding movement should not receive walk-speed boosts.
        val player = event.player
        if (player.isGliding) {
            return
        }

        var effectEntity: LivingEntity = player
        if (player.isInsideVehicle && player.vehicle is LivingEntity) {
            effectEntity = player.vehicle as LivingEntity
        }

        // Sample the block slightly below the feet to identify the walked material.
        applyWalkSpeedIfOnMaterial(effectEntity, effectEntity.location)
    }

    /**
     * Samples the block slightly below the given location and applies the configured walk-speed
     * potion effect to the provided entity if the block's material matches the configured path
     * materials.
     */
    private fun applyWalkSpeedIfOnMaterial(
        entity: org.bukkit.entity.Entity,
        sampleLocation: Location,
    ) {
        if (entity !is LivingEntity) return

        val block = sampleLocation.clone().subtract(0.0, 0.1, 0.0).block
        val materials = fastWalking.configMaterials
        if (block.type !in materials) return

        val walkSpeedEffect = fastWalking.walkSpeedEffect ?: return
        entity.addPotionEffect(walkSpeedEffect)
    }

    /** Applies the fast-walk effect to non-player entities when enabled by config. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityMove(event: EntityMoveEvent) {
        val entity = event.entity

        // Cancel event if speedwalking is only enabled for players
        if (configPlayersOnlySpeedwalk) return

        // Cancel event if speedwalking is disabled for Hostile mobs
        if (entity is Monster && !configHostileSpeedwalk) return

        // Cancel event if speedwalking is disabled for villagers
        if (entity.type == EntityType.VILLAGER && !configVillagerSpeedwalk) return

        applyWalkSpeedIfOnMaterial(entity, event.to)
    }
}
