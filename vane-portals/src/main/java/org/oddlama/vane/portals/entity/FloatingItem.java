package org.oddlama.vane.portals.entity;

import static org.oddlama.vane.util.Nms.world_handle;

import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.EntityHuman;
import net.minecraft.server.v1_16_R3.EntityItem;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.ItemStack;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.World;

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
		s(); // setNoPickup(); (same as: pickupDelay = 32767;)
		persist = false;
		noclip = true;
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
	@Override public boolean a_(NBTTagCompound nbttagcompound) { return false; }
	@Override public boolean d(NBTTagCompound nbttagcompound) { return false; }
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
