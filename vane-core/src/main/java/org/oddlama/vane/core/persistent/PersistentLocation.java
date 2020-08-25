package org.oddlama.vane.core.persistent;

import java.io.Serializable;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class PersistentLocation implements Serializable {
    private UUID world_id;
    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;

	public Location get() {
		return new Location(Bukkit.getWorld(world_id), x, y, z, yaw, pitch);
	}

	public PersistentLocation set(Location location) {
		this.world_id = location.getWorld().getUID();
		this.x = location.getX();
		this.y = location.getY();
		this.z = location.getZ();
		this.pitch = location.getPitch();
		this.yaw = location.getYaw();
		return this;
	}

	public static PersistentLocation from(Location location) {
		return new PersistentLocation().set(location);
	}
}
