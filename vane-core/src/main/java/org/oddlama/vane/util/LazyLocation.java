package org.oddlama.vane.util;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LazyLocation {

	private final UUID world_id;
	private Location location;

	public LazyLocation(final Location location) {
		this.world_id = location.getWorld() == null ? null : location.getWorld().getUID();
		this.location = location.clone();
	}

	public LazyLocation(final UUID world_id, double x, double y, double z, float pitch, float yaw) {
		this.world_id = world_id;
		this.location = new Location(null, x, y, z, pitch, yaw);
	}

	public UUID world_id() {
		return world_id;
	}

	public Location location() {
		if (world_id != null && location.getWorld() == null) {
			location.setWorld(Bukkit.getWorld(world_id));
		}

		return location;
	}
}
