package org.oddlama.vane.regions;

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

@VaneModule(name = "regions", bstats = 8643, config_version = 2, lang_version = 2, storage_version = 1)
public class Regions extends Module<Regions> {
	//
	//                                                  ┌───────────────────────┐
	// ┌────────────┐  is   ┌───────────────┐         ┌───────────────────────┐ |  belongs to  ┌─────────────────┐
	// |  Player 1  | ────> | [Role] Admin  | ───┬──> | [RegionGroup] Default |─┘ <───┬─────── | [Region] MyHome |
	// └────────────┘       └───────────────┘    |    └───────────────────────┘       |        └─────────────────┘
	//                                           |                                    |
	// ┌────────────┐  in   ┌───────────────┐    |                                    |        ┌─────────────────────┐
	// |  Player 2  | ────> | [Role] Friend | ───┤ (are roles of)                     └─────── | [Region] Drecksloch |
	// └────────────┘       └───────────────┘    |                                             └─────────────────────┘
	//                                           |
	// ┌────────────┐  in   ┌───────────────┐    |
	// | Any Player | ────> | [Role] Others | ───┘
	// └────────────┘       └───────────────┘
	//
	// General Menu: /rg
	// row 1: General
	// 1. create new region
	// 2. create new group
	// 5. all regions where i am admin
	// 6. all groups where i am admin
	// 8. shortcut to current region group if any
	// 9. shortcut to current region if any
	//
	//
	// Menu: Region
	// 1. edit name
	// 2. set group
	// 3. delete
	//
	// Menu: Group
	// 1. edit name
	// 2. add role
	// 3. roles
	// 3. delete
	//
	// Menu: Role
	// 1. edit name (if not special)
	// 2. players (if not others)
	// 2. delete role (if not special)

	// Primary storage for all regions (region.id → region)
	@Persistent
	private Map<UUID, Region> storage_regions = new HashMap<>();
	// Primary storage for all region_groups (region_group.id → region_group)
	@Persistent
	private Map<UUID, RegionGroup> storage_region_groups = new HashMap<>();
	// Primary storage for the default region groups for new regions created by a player (player_uuid → region_group.id)
	@Persistent
	private Map<UUID, UUID> storage_default_region_group = new HashMap<>();

	// Per-chunk lookup cache
	private Map<Long, List<UUID>> regions_for_chunk = new HashMap<>();

	public Regions() {
		//menus = new RegionMenuGroup(this);
		//dynmap_layer = new RegionDynmapLayer(this);
	}

	public void add_region_group(final RegionGroup group) {
		storage_region_groups.put(group.id(), group);
		mark_persistent_storage_dirty();
	}

	public void add_region(final Region region) {
		storage_regions.put(region.id(), region);
		mark_persistent_storage_dirty();
	}
}
