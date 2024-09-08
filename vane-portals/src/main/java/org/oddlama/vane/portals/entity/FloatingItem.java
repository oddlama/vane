package org.oddlama.vane.portals.entity;

import static org.oddlama.vane.util.Nms.world_handle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.World;

public class FloatingItem extends ItemEntity {

	public FloatingItem(final Location location) {
		this(location.getWorld(), location.getX(), location.getY(), location.getZ());
	}

	public FloatingItem(final World world, double x, double y, double z) {
		this(EntityType.ITEM, world_handle(world));
		setPos(x, y, z);
	}

	public FloatingItem(EntityType<? extends ItemEntity> entitytypes, Level world) {
		super(entitytypes, world);
		setSilent(true);
		setInvulnerable(true);
		setNoGravity(true);
		//setSneaking(true); // Names would then only visible on direct line of sight BUT much darker and offset by -0.5 in y direction
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
	public boolean isInvulnerableTo(DamageSource source) {
		return true;
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
	public void readAdditionalSaveData(CompoundTag nbt) {}

	@Override
	public void addAdditionalSaveData(CompoundTag nbt) {}

	@Override
	public boolean serializeEntity(CompoundTag nbt) {
		return false;
	}

	@Override
	public boolean save(CompoundTag nbt) {
		return false;
	}

	@Override
	public CompoundTag saveWithoutId(CompoundTag nbt) {
		return nbt;
	}

	@Override
	public void load(CompoundTag nbt) {}

	@Override
	public void setItem(ItemStack itemStack) {
		super.setItem(itemStack);
		if (itemStack.getHoverName().toFlatList().size() > 0) {
			setCustomNameVisible(true);
			setCustomName(itemStack.getHoverName());
		} else setCustomNameVisible(false);
	}
}
