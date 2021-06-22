package org.oddlama.vane.portals.entity;

import static org.oddlama.vane.util.Nms.world_handle;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.item.EntityItem;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.level.World;

import org.bukkit.Location;

public class FloatingItem extends EntityItem {
	public FloatingItem(final Location location) {
		this(location.getWorld());
		setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
	}

	public FloatingItem(final org.bukkit.World world, double x, double y, double z) {
		this(world);
		setLocation(x, y, z, 0, 0);
	}

	public FloatingItem(final org.bukkit.World world) {
		this(world_handle(world));
	}

	public FloatingItem(final World world) {
		this(EntityTypes.ITEM, world);
	}

	public FloatingItem(EntityTypes<EntityItem> entitytypes, World world) {
		super(entitytypes, world);

		setSilent(true);
		setInvulnerable(true);
		setNoGravity(true);
		//setSneaking(true); // Names would then only visible on direct line of sight BUT much darker and offset by -0.5 in y direction
		p(); // setNoPickup(); (same as: pickupDelay = 32767;)
		persist = false;
		noPhysics = true;
	}

	@Override public boolean isInteractable() { return false; }
	@Override public boolean isCollidable() { return false; }
	@Override public boolean damageEntity(DamageSource damagesource, float f) { return false; }
	@Override public boolean isInvulnerable(DamageSource source) { return true; }
	@Override public boolean isInvisible() { return true; }

	@Override
	public void tick() {
		// Not ticking
	}

	@Override
	public void inactiveTick() {
		// No entity ticking
	}

	// Don't save or load
	@Override public void saveData(NBTTagCompound nbttagcompound) {}
	@Override public void loadData(NBTTagCompound nbttagcompound) {}
	@Override public boolean d(NBTTagCompound nbttagcompound) { return false; }
	@Override public boolean e(NBTTagCompound nbttagcompound) { return false; }
	@Override public NBTTagCompound save(NBTTagCompound nbttagcompound) { return nbttagcompound; }
	@Override public void load(NBTTagCompound nbttagcompound) {}

	@Override
	public void setItemStack(ItemStack itemStack) {
		super.setItemStack(itemStack);

		if (itemStack.hasName()) {
			setCustomNameVisible(true);
			setCustomName(itemStack.getName());
		} else
			setCustomNameVisible(false);
	}

	@Override
	public void pickup(EntityHuman entityhuman) {
		// No pickup
	}
}
