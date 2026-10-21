package org.oddlama.vane.trifles

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.attribute.Attributes
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes.PropertyModifier
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.github.retrooper.packetevents.util.SpigotReflectionUtil
import java.util.*
import org.bukkit.block.Container
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask

/** Packet-level visualization helpers for item finder search matches. */
object ItemFinderPacketUtils {
    /** Duration in ticks for temporary highlight indicators. */
    private const val GLOW_DURATION = 20L * 8L

    /** Default model scale used for generic block-display container indicators. */
    private const val CONTAINER_DEFAULT_SCALE = 0.95f

    /** Translation that keeps scaled block displays centered on their source block. */
    private const val CONTAINER_DEFAULT_TRANSLATION = (1 - CONTAINER_DEFAULT_SCALE) * 0.5f

    /** Default model scale used for chest indicators. */
    private const val CHEST_DEFAULT_SCALE = 0.85f

    /** Sends a packet to a single player through PacketEvents. */
    private fun sendPacket(player: Player, packet: PacketWrapper<*>) {
        PacketEvents.getAPI().playerManager.sendPacket(player, packet)
    }

    /** Builds a temporary scale attribute packet for a spawned display entity. */
    private fun createScalePacket(displayId: Int, scale: Float): WrapperPlayServerUpdateAttributes {
        val attributeModifier =
            PropertyModifier(
                UUID.randomUUID(),
                (scale - 1).toDouble(),
                PropertyModifier.Operation.ADDITION,
            )
        val attributeProperty =
            WrapperPlayServerUpdateAttributes.Property(
                Attributes.SCALE,
                1.0,
                mutableListOf<PropertyModifier?>(attributeModifier),
            )
        return WrapperPlayServerUpdateAttributes(
            displayId,
            mutableListOf<WrapperPlayServerUpdateAttributes.Property?>(attributeProperty),
        )
    }

    /** Highlights a matching entity by forcing a temporary glow state client-side. */
    fun indicateEntityMatch(module: Trifles, player: Player, entity: Entity) {
        // Capture base metadata before applying a glowing override.
        val baseEntityData = SpigotConversionUtil.getEntityMetadata(entity)

        // Build glowing metadata payload.
        val glowingEntityData = ArrayList(baseEntityData)
        val glowData = EntityData(0, EntityDataTypes.BYTE, 0x40.toByte())
        glowingEntityData.add(glowData)

        // Create glowing metadata packet.
        val glowingPacket =
            WrapperPlayServerEntityMetadata(
                entity.entityId,
                glowingEntityData,
            )

        // Send immediate glow update.
        sendPacket(player, glowingPacket)

        // Refresh glow every tick to keep client-side state stable while visible.
        val repeatingPacketTask =
            object : BukkitRunnable() {
                    override fun run() {
                        if (!player.isOnline) {
                            this.cancel()
                        }

                        sendPacket(player, glowingPacket)
                    }
                }
                .runTaskTimer(module, 1, 1)

        // Prepare reset metadata packet.
        val nonGlowingEntityData = ArrayList(baseEntityData)
        val nonGlowingPacket =
            WrapperPlayServerEntityMetadata(
                entity.entityId,
                nonGlowingEntityData,
            )

        // Schedule reset packet after highlight duration.
        ResetGlowingPacketTask(player, nonGlowingPacket, repeatingPacketTask)
            .runTaskLater(module, GLOW_DURATION)
    }

    /** Spawns a temporary marker for non-chest container matches. */
    fun indicateContainerMatch(module: Trifles, player: Player, container: Container) {
        val displayId = SpigotReflectionUtil.generateEntityId()
        // Use shulker entity type for shulker boxes, otherwise use block display.
        val entityType =
            if (container is ShulkerBox) EntityTypes.SHULKER else EntityTypes.BLOCK_DISPLAY
        val displayPacket =
            WrapperPlayServerSpawnEntity(
                displayId,
                UUID.randomUUID(),
                entityType,
                SpigotConversionUtil.fromBukkitLocation(container.location),
                container.location.yaw,
                0,
                null,
            )

        // Build display metadata payload.
        val displayMetadata =
            mutableListOf<EntityData<*>?>(
                EntityData(0, EntityDataTypes.BYTE, (0x20 or 0x40).toByte())
            )
        if (entityType === EntityTypes.BLOCK_DISPLAY) {
            displayMetadata.add(
                EntityData(
                    11,
                    EntityDataTypes.VECTOR3F,
                    Vector3f(
                        CONTAINER_DEFAULT_TRANSLATION,
                        CONTAINER_DEFAULT_TRANSLATION,
                        CONTAINER_DEFAULT_TRANSLATION,
                    ),
                )
            )
            displayMetadata.add(
                EntityData(
                    12,
                    EntityDataTypes.VECTOR3F,
                    Vector3f(
                        CONTAINER_DEFAULT_SCALE,
                        CONTAINER_DEFAULT_SCALE,
                        CONTAINER_DEFAULT_SCALE,
                    ),
                )
            )
            displayMetadata.add(
                EntityData(
                    23,
                    EntityDataTypes.BLOCK_STATE,
                    SpigotConversionUtil.fromBukkitBlockData(container.blockData).globalId,
                )
            )
        }
        val displayMetadataPacket =
            WrapperPlayServerEntityMetadata(
                displayId,
                displayMetadata,
            )

        // Spawn and configure temporary marker entity.
        sendPacket(player, displayPacket)
        sendPacket(player, displayMetadataPacket)

        // Apply scale through attribute packet for shulker marker entities.
        if (entityType === EntityTypes.SHULKER) {
            sendPacket(player, createScalePacket(displayId, CONTAINER_DEFAULT_SCALE))
        }

        // Prepare marker cleanup packet.
        val destroyEntityPacket = WrapperPlayServerDestroyEntities(displayId)

        // Despawn marker after highlight duration.
        ResetGlowingPacketTask(player, destroyEntityPacket, null)
            .runTaskLater(module, GLOW_DURATION)
    }

    /** Spawns a temporary marker tuned for chest matches. */
    fun indicateChestMatch(module: Trifles, player: Player, container: Container) {
        val displayId = SpigotReflectionUtil.generateEntityId()
        val displayPacket =
            WrapperPlayServerSpawnEntity(
                displayId,
                UUID.randomUUID(),
                EntityTypes.SHULKER,
                SpigotConversionUtil.fromBukkitLocation(container.location),
                container.location.yaw,
                0,
                null,
            )

        // Build chest marker metadata payload.
        val displayMetadata =
            mutableListOf<EntityData<*>?>(
                EntityData(0, EntityDataTypes.BYTE, (0x20 or 0x40).toByte())
            )
        val displayMetadataPacket =
            WrapperPlayServerEntityMetadata(
                displayId,
                displayMetadata,
            )

        // Spawn and configure temporary chest marker.
        sendPacket(player, displayPacket)
        sendPacket(player, displayMetadataPacket)

        // Apply chest-specific scale via shulker attribute update.
        sendPacket(player, createScalePacket(displayId, CHEST_DEFAULT_SCALE))

        // Prepare chest marker cleanup packet.
        val destroyEntityPacket = WrapperPlayServerDestroyEntities(displayId)

        // Despawn marker after highlight duration.
        ResetGlowingPacketTask(player, destroyEntityPacket, null)
            .runTaskLater(module, GLOW_DURATION)
    }

    /** Delayed task that clears temporary visuals and optionally stops a repeating packet task. */
    private class ResetGlowingPacketTask(
        /** Player that receives the reset packet. */
        private val player: Player,

        /** Packet sent when the visual indicator should be removed. */
        private val packet: PacketWrapper<*>,

        /** Optional repeating task used for persistent glow refreshes. */
        private val optionalRepeatingPacketTask: BukkitTask?,
    ) : BukkitRunnable() {
        /** Sends the reset packet and cancels any repeating helper task. */
        override fun run() {
            if (!player.isOnline) {
                this.cancel()
            }

            if (optionalRepeatingPacketTask != null && !optionalRepeatingPacketTask.isCancelled) {
                optionalRepeatingPacketTask.cancel()
            }

            sendPacket(player, packet)
        }
    }
}
