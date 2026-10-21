package org.oddlama.vane.portals.entity

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.bukkit.Location
import org.bukkit.World
import org.oddlama.vane.util.Nms

/**
 * Non-pickupable, non-persistent item entity used as a floating display item.
 *
 * @param entitytypes underlying NMS entity type.
 * @param world NMS world handle.
 */
class FloatingItem(entitytypes: EntityType<out ItemEntity>, world: Level) :
    ItemEntity(
        entitytypes,
        world,
    ) {
    /** Creates a floating item at the given Bukkit [location]. */
    constructor(location: Location) : this(location.world, location.x, location.y, location.z)

    /** Creates a floating item in [world] at exact coordinates. */
    constructor(
        world: World,
        x: Double,
        y: Double,
        z: Double,
    ) : this(EntityTypes.ITEM, Nms.worldHandle(world)) {
        setPos(x, y, z)
    }

    init {
        isSilent = true
        isPermanentlyInvulnerable = true
        isNoGravity = true
        // setSneaking(true); // Names would then only visible on direct line of sight BUT much
        // darker and offset by -0.5 in y direction
        setNeverPickUp()
        setUnlimitedLifetime()
        persist = false
        noPhysics = true
    }

    /** Always returns false so hoppers cannot pick this item up. */
    override fun isAlive(): Boolean {
        // Required to efficiently prevent hoppers and hopper minecarts from picking this up
        return false
    }

    /** Returns false because this display entity cannot be attacked. */
    override fun isAttackable() = false

    /** Returns false because this display entity does not collide. */
    override fun isCollidable(ignoreClimbing: Boolean) = false

    /** Returns true because the item body should stay invisible. */
    override fun isInvisible() = true

    /** Returns true because this display entity is fire-immune. */
    override fun fireImmune() = true

    /** Disables normal ticking for this display entity. */
    override fun tick() {}

    /** Disables inactive ticking for this display entity. */
    override fun inactiveTick() {}

    /** No-op: this entity is intentionally not persisted. */
    public override fun readAdditionalSaveData(output: ValueInput) {}

    /** No-op: this entity is intentionally not persisted. */
    override fun addAdditionalSaveData(output: ValueOutput) {}

    /** Always returns false because this entity should never be saved. */
    override fun save(output: ValueOutput): Boolean {
        return false
    }

    /** No-op: this entity is intentionally not persisted. */
    override fun saveWithoutId(output: ValueOutput) {}

    /** No-op: this entity is intentionally not loaded from the disk. */
    override fun load(output: ValueInput) {}

    /** Sets the displayed item and mirrors its hover name as entity custom name. */
    override fun setItem(itemStack: ItemStack) {
        super.setItem(itemStack)
        isCustomNameVisible = itemStack.hoverName.toFlatList().isNotEmpty()
        if (isCustomNameVisible) {
            customName = itemStack.hoverName
        }
    }
}
