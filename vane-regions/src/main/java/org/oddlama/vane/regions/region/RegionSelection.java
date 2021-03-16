package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;
import static org.oddlama.vane.util.BlockUtil.unpack;
import static org.oddlama.vane.util.ItemUtil.name_item;
import static org.oddlama.vane.util.Nms.item_handle;
import static org.oddlama.vane.util.Nms.register_entity;
import static org.oddlama.vane.util.PlayerUtil.has_items;
import static org.oddlama.vane.util.Nms.spawn;
import static org.oddlama.vane.util.Util.ms_to_ticks;
import static org.oddlama.vane.util.Util.namespaced_key;

import org.oddlama.vane.external.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.EnumCreatureType;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMapEntry;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.material.ExtendedMaterial;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.util.LazyBlock;
import org.oddlama.vane.regions.Regions;

public class RegionSelection {
	private Regions regions;
	public Block primary = null;
	public Block secondary = null;

	public RegionSelection(final Regions regions) {
		this.regions = regions;
	}

	public boolean intersects_existing() {
		final var extent = extent();
		for (final var r : regions.all_regions()) {
			if (!r.extent().min().getWorld().equals(primary.getWorld())) {
				continue;
			}

			if (extent.intersects_extent(r.extent())) {
				return true;
			}
		}

		return false;
	}

	public int price() {
		final var dx = 1 + Math.abs(primary.getX() - secondary.getX());
		final var dy = 1 + Math.abs(primary.getY() - secondary.getY());
		final var dz = 1 + Math.abs(primary.getZ() - secondary.getZ());
		return (int)Math.ceil(Math.pow(regions.config_cost_y_multiplicator, dy / 16.0) * regions.config_cost_xz_base / 256.0 * dx * dz);
	}

	public boolean can_afford(final Player player) {
		final var price = price();
		if (price <= 0) {
			return true;
		}
		final var map = new HashMap<ItemStack, Integer>();
		map.put(new ItemStack(regions.config_currency), price);
		return has_items(player, map);
	}

	public boolean is_valid(final Player player) {
		// Both blocks set
		if (primary == null || secondary == null) {
			return false;
		}

		// Worlds match
		if (!primary.getWorld().equals(secondary.getWorld())) {
			return false;
		}

		final var dx = 1 + Math.abs(primary.getX() - secondary.getX());
		final var dy = 1 + Math.abs(primary.getY() - secondary.getY());
		final var dz = 1 + Math.abs(primary.getZ() - secondary.getZ());

		// min <= extent <= max
		if (dx < regions.config_min_region_extent_x ||
			dy < regions.config_min_region_extent_y ||
			dz < regions.config_min_region_extent_z ||
			dx > regions.config_max_region_extent_x ||
			dy > regions.config_max_region_extent_y ||
			dz > regions.config_max_region_extent_z) {
			return false;
		}

		// Assert that it doesn't intersect an existing region
		if (intersects_existing()) {
			return false;
		}

		// Check that the player can afford it
		if (!can_afford(player)) {
			return false;
		}

		return true;
	}

	public RegionExtent extent() {
		return new RegionExtent(primary, secondary);
	}
}
