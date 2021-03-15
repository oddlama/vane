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

public class Region {
	public static Object serialize(@NotNull final Object o) throws IOException {
		final var region = (Region)o;
		final var json = new JSONObject();
		json.put("id",           to_json(UUID.class,         region.id));
		json.put("name",         to_json(String.class,       region.name));
		json.put("owner",        to_json(UUID.class,         region.owner));
		json.put("region_group", to_json(UUID.class,         region.region_group));
		json.put("extent",       to_json(RegionExtent.class, region.extent));
		return json;
	}

	@SuppressWarnings("unchecked")
	public static Region deserialize(@NotNull final Object o) throws IOException {
		final var json = (JSONObject)o;
		final var region = new Region();
		region.id           = from_json(UUID.class,         json.get("id"));
		region.name         = from_json(String.class,       json.get("name"));
		region.owner        = from_json(UUID.class,         json.get("owner"));
		region.region_group = from_json(UUID.class,         json.get("region_group"));
		region.extent       = from_json(RegionExtent.class, json.get("extent"));
		return region;
	}

	private Region() {}
	public Region(final String name, final UUID owner, final RegionExtent extent, final UUID region_group) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.owner = owner;
		this.extent = extent;
		this.region_group = region_group;
	}

	private UUID id;
	private String name;
	private UUID owner;
	private RegionExtent extent;
	private UUID region_group;

	public UUID id() { return id; }
	public String name() { return name; }
	public void name(final String name) { this.name = name; }
	public UUID owner() { return owner; }
	public RegionExtent extent() { return extent; }

	private RegionGroup cached_region_group = null;
	public UUID region_group_id() { return region_group; }
	public void region_group_id(final UUID region_group) { this.region_group = region_group; }
	public RegionGroup region_group(final Regions regions) {
		if (cached_region_group == null) {
			cached_region_group = regions.get_region_group(region_group);
		}
		return cached_region_group;
	}
}
