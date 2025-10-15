package org.oddlama.vane.portals.entity;

import static org.oddlama.vane.util.Nms.world_handle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class FloatingItem extends ItemEntity {

    public FloatingItem(final Location location) {
        this(location.getWorld(), location.getX(), location.getY(), location.getZ());
    }

    public FloatingItem(final World world, double x, double y, double z) {
        this(EntityType.ITEM, world_handle(world));
        setPos(x, y, z);
    }

    public FloatingItem(EntityType<?> entitytypes, Level world) {
        super((EntityType<? extends ItemEntity>)entitytypes, world);
        setSilent(true);
        setInvulnerable(true);
        setNoGravity(true);
        // setSneaking(true); // Names would then only visible on direct line of sight BUT much
        // darker and offset by -0.5 in y direction
        setNeverPickUp();
        setUnlimitedLifetime();
        persist = false;
        noPhysics = true;
    }

    @Override
    public boolean isAlive() {
        // Required to efficiently prevent hoppers and hopper minecarts from picking this up
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isCollidable(boolean ignoreClimbing) {
        return false;
    }

    @Override
    public boolean isInvisible() {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {}

    @Override
    public void inactiveTick() {}

    // Don't save or load
    @Override
    public void readAdditionalSaveData(@NotNull ValueInput output) {}

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {}

    @Override
    public boolean save(@NotNull ValueOutput output) {
        return false;
    }

    @Override
    public void saveWithoutId(@NotNull ValueOutput output) {}

    @Override
    public void load(@NotNull ValueInput output) {}

    @Override
    public void setItem(@NotNull ItemStack itemStack) {
        super.setItem(itemStack);
        if (itemStack.getHoverName().toFlatList().isEmpty()) {
            setCustomNameVisible(false);
        } else {
            setCustomNameVisible(true);
            setCustomName(itemStack.getHoverName());
        }
    }
}