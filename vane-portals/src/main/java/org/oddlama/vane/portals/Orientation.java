package org.oddlama.vane.portals;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import java.util.HashMap;
import java.util.Iterator;
import org.bukkit.event.block.Action;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import org.oddlama.vane.annotation.VaneModule;
import org.jetbrains.annotations.Nullable;
import org.oddlama.vane.core.module.Module;

public enum Orientation {
	POSITIVE_X(Plane.YZ, new Vector(1, 0, 0)),
	NEGATIVE_X(Plane.YZ, new Vector(-1, 0, 0)),
	POSITIVE_Y(Plane.XZ, new Vector(0, 1, 0)),
	NEGATIVE_Y(Plane.XZ, new Vector(0, -1, 0)),
	POSITIVE_Z(Plane.XY, new Vector(0, 0, 1)),
	NEGATIVE_Z(Plane.XY, new Vector(0, 0, -1));

	private Plane plane;
	private Vector vector;
	private Orientation(Plane plane, Vector vector) {
		this.plane = plane;
		this.vector = vector;
	}

	public Plane plane() {
		return plane;
	}

	public Vector vector() {
		return vector;
	}

	public Location apply(final Orientation reference, final Location location) {
		final var l = location.clone();
		l.setDirection(apply(reference, location.getDirection()));
		return l;
	}

	public Vector apply(final Orientation reference, final Vector vector) {
		return vector;
	}

	public static Orientation getOrientation(final Plane plane, final Block origin, final Block console, final Location entity_location) {
		switch (plane) {
			case XY: {
				final var origin_z = origin.getZ() + 0.5;
				final var console_z = console.getZ() + 0.5;
				if (console_z > origin_z) {
					return NEGATIVE_Z;
				} else if (console_z < origin_z) {
					return POSITIVE_Z;
				} else {
					if (entity_location.getZ() > origin_z) {
						return NEGATIVE_Z;
					} else {
						return POSITIVE_Z;
					}
				}
			}

			case YZ: {
				final var origin_x = origin.getX() + 0.5;
				final var console_x = console.getX() + 0.5;
				if (console_x > origin_x) {
					return NEGATIVE_X;
				} else if (console_x < origin_x) {
					return POSITIVE_X;
				} else {
					if (entity_location.getX() > origin_x) {
						return NEGATIVE_X;
					} else {
						return POSITIVE_X;
					}
				}
			}

			case XZ: {
				final var origin_y = origin.getY() + 0.5;
				final var console_y = console.getY() + 0.5;
				if (console_y >= origin_y) {
					return NEGATIVE_Y;
				} else { /* if (console_y < origin_y) */
					return POSITIVE_Y;
				}
			}
		}

		// Unreachable
		throw new RuntimeException("Invalid control flow. This is a bug.");
	}
}
