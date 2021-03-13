package org.oddlama.vane.regions.region;

import static org.oddlama.vane.core.persistent.PersistentSerializer.from_json;
import static org.oddlama.vane.core.persistent.PersistentSerializer.to_json;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;
import static org.oddlama.vane.util.BlockUtil.unpack;
import static org.oddlama.vane.util.ItemUtil.name_item;
import static org.oddlama.vane.util.Nms.item_handle;
import static org.oddlama.vane.util.Nms.register_entity;
import static org.oddlama.vane.util.Nms.spawn;
import static org.oddlama.vane.util.Util.ms_to_ticks;
import static org.oddlama.vane.util.Util.namespaced_key;

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

public class RegionExtent {
	public static Object serialize(@NotNull final Object o) throws IOException {
		final var region_extent = (RegionExtent)o;
		final var json = new JSONObject();
		json.put("min", to_json(LazyBlock.class, region_extent.min));
		json.put("max", to_json(LazyBlock.class, region_extent.max));
		return json;
	}

	public static RegionExtent deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject)o;
		final var min = from_json(LazyBlock.class, json.get("min"));
		final var max = from_json(LazyBlock.class, json.get("max"));
		return new RegionExtent(min, max);
	}

	// Both inclusive, so we don't run into errors with
	// blocks outside of the world (y<min_height || y>max_height).
	// Also, coordinates are sorted, so min is always the smaller coordinate on each axis.
	// For each x,y,z: min.[x,y,z] <= max.[x,y,z]
	private LazyBlock min; // inclusive
	private LazyBlock max;   // inclusive

	public RegionExtent(final LazyBlock min, final LazyBlock max) {
		this.min = min;
		this.max = max;
	}

	public RegionExtent(final Block from, final Block to) {
		if (!from.getWorld().equals(to.getWorld())) {
			throw new RuntimeException("Invalid region extent across dimensions!");
		}

		// Sort coordinates along axes.
		this.min = new LazyBlock(new Block(from.getWorld(),
				Math.min(from.getX(), to.getX()),
				Math.min(from.getY(), to.getY()),
				Math.min(from.getZ(), to.getZ())));
		this.max = new LazyBlock(new Block(from.getWorld(),
				Math.max(from.getX(), to.getX()),
				Math.max(from.getY(), to.getY()),
				Math.max(from.getZ(), to.getZ())));
	}

	public Block min() { return min.block(); }
	public Block max() { return max.block(); }

	public boolean is_block_inside(final Block block) {
		if (!block.getWorld().equals(min().getWorld())) {
			return false;
		}

		// TODO check
	}

	public boolean intersects_chunk(final Chunk chunk) {
		if (!chunk.getWorld().equals(min().getWorld())) {
			return false;
		}

		// TODO return true iff any block inside extent is inside chunk.
	}
}
