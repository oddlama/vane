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
import java.util.List;
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
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
import org.oddlama.vane.regions.region.Region;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.RegionSelection;
import org.oddlama.vane.regions.region.Role;
import org.oddlama.vane.regions.region.RegionExtent;
import org.oddlama.vane.regions.region.RoleSetting;
import org.oddlama.vane.regions.menu.RegionGroupMenuTag;
import org.oddlama.vane.regions.menu.RegionMenuGroup;
import org.oddlama.vane.regions.menu.RegionMenuTag;
import org.oddlama.vane.regions.region.EnvironmentSetting;

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
	// Menu: Region
	// 1. edit name
	// 2. set group
	// 3. delete
	//
	// Menu: Group
	// 1. edit name
	// 2. add role
	// 3. roles
	// 4. delete
	//
	// Menu: Role
	// 1. edit name (if not special)
	// 2. players (if not others)
	// 3. delete role (if not special)

	// Add (de-)serializers
	static {
		PersistentSerializer.serializers.put(EnvironmentSetting.class,    x -> ((EnvironmentSetting)x).name());
		PersistentSerializer.deserializers.put(EnvironmentSetting.class,  x -> EnvironmentSetting.valueOf((String)x));
		PersistentSerializer.serializers.put(RoleSetting.class,           x -> ((RoleSetting)x).name());
		PersistentSerializer.deserializers.put(RoleSetting.class,         x -> RoleSetting.valueOf((String)x));
		PersistentSerializer.serializers.put(Role.class,                  Role::serialize);
		PersistentSerializer.deserializers.put(Role.class,                Role::deserialize);
		PersistentSerializer.serializers.put(Role.RoleType.class,         x -> ((Role.RoleType)x).name());
		PersistentSerializer.deserializers.put(Role.RoleType.class,       x -> Role.RoleType.valueOf((String)x));
		PersistentSerializer.serializers.put(RegionGroup.class,           RegionGroup::serialize);
		PersistentSerializer.deserializers.put(RegionGroup.class,         RegionGroup::deserialize);
		PersistentSerializer.serializers.put(Region.class,                Region::serialize);
		PersistentSerializer.deserializers.put(Region.class,              Region::deserialize);
		PersistentSerializer.serializers.put(RegionExtent.class,          RegionExtent::serialize);
		PersistentSerializer.deserializers.put(RegionExtent.class,        RegionExtent::deserialize);
	}

	@ConfigInt(def = 4, min = 1, desc = "Minimum region extent in x direction.")
	public int config_min_region_extent_x;
	@ConfigInt(def = 4, min = 1, desc = "Minimum region extent in y direction.")
	public int config_min_region_extent_y;
	@ConfigInt(def = 4, min = 1, desc = "Minimum region extent in z direction.")
	public int config_min_region_extent_z;

	@ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in x direction.")
	public int config_max_region_extent_x;
	@ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in y direction.")
	public int config_max_region_extent_y;
	@ConfigInt(def = 2048, min = 1, desc = "Maximum region extent in z direction.")
	public int config_max_region_extent_z;

	// Primary storage for all regions (region.id → region)
	@Persistent
	private Map<UUID, Region> storage_regions = new HashMap<>();
	// Primary storage for all region_groups (region_group.id → region_group)
	@Persistent
	private Map<UUID, RegionGroup> storage_region_groups = new HashMap<>();
	// Primary storage for the default region groups for new regions created by a player (player_uuid → region_group.id)
	@Persistent
	private Map<UUID, UUID> storage_default_region_group = new HashMap<>();

	// Per-chunk lookup cache (world_id → chunk_key → [possible regions])
	private Map<UUID, Map<Long, List<Region>>> regions_in_chunk_in_world = new HashMap<>();
	// A map containing the current extent for each player who is currently selecting a region
	// No key → Player not in selection mode
	// extent.min or extent.max null → Selection mode active, but no selection has been made yet
	private Map<UUID, RegionSelection> regions_selections = new HashMap<>();

	@LangMessage public TranslatedMessage lang_start_region_selection;

	public RegionMenuGroup menus;

	public Regions() {
		menus = new RegionMenuGroup(this);

		new org.oddlama.vane.regions.commands.Region(this);

		//dynmap_layer = new RegionDynmapLayer(this);
		new RegionEnvironmentSettingEnforcer(this);
		new RegionRoleSettingEnforcer(this);
		new RegionSelectionListener(this);
	}

	public void delayed_on_enable() {
		for (var region : storage_regions.values()) {
			index_add_region(region);
		}
	}

	@Override
	public void on_enable() {
		schedule_next_tick(this::delayed_on_enable);
	}

	public Collection<Region> all_regions() {
		return storage_regions.values();
	}

	public Collection<RegionGroup> all_region_groups() {
		return storage_region_groups.values();
	}

	public void start_region_selection(final Player player) {
		regions_selections.put(player.getUniqueId(), new RegionSelection(this));
		lang_start_region_selection.send(player);
	}

	public void cancel_region_selection(final Player player) {
		regions_selections.remove(player.getUniqueId());
	}

	public boolean is_selecting_region(final Player player) {
		return regions_selections.containsKey(player.getUniqueId());
	}

	public RegionSelection get_region_selection(final Player player) {
		return regions_selections.get(player.getUniqueId());
	}

	public void add_region_group(final RegionGroup group) {
		storage_region_groups.put(group.id(), group);
		mark_persistent_storage_dirty();
	}

	public boolean can_remove_region_group(final RegionGroup group) {
		// Returns true if this region group is unused and can be removed.

		// If this region group is the fallback default group, it is permanent!
		if (storage_default_region_group.values().contains(group.id())) {
			return false;
		}

		// If any region uses this group, we can't remove it.
		if (storage_regions.values().stream().anyMatch(
			r -> r.region_group_id().equals(group.id()))) {
			return false;
		}

		return true;
	}

	public void remove_region_group(final RegionGroup group) {
		// Assert that this region group is unused.
		if (!can_remove_region_group(group)) {
			return;
		}

		// Remove region group from storage
		if (storage_region_groups.remove(group.id()) == null) {
			// Was already removed
			return;
		}

		mark_persistent_storage_dirty();

		// Close and taint all related open menus
		get_module().core.menu_manager.for_each_open((player, menu) -> {
			if (menu.tag() instanceof RegionGroupMenuTag
					&& Objects.equals(((RegionGroupMenuTag)menu.tag()).region_group_id(), group.id())) {
				menu.taint();
				menu.close(player);
			}
		});
	}

	public RegionGroup get_region_group(final UUID region_group) {
		return storage_region_groups.get(region_group);
	}

	public boolean create_region_from_selection(final Player player, final String name) {
		final var selection = get_region_selection(player);
		if (!selection.is_valid(player)) {
			return false;
		}

		final var def_region_group = get_or_create_default_region_group(player.getUniqueId());
		final var region = new Region(name, player.getUniqueId(), selection.extent(), def_region_group.id());
		add_region(region);
		cancel_region_selection(player);
		return true;
	}

	public void add_region(final Region region) {
		storage_regions.put(region.id(), region);
		mark_persistent_storage_dirty();

		// Index region for fast lookup
		index_add_region(region);

		// Create dynmap marker
		//dynmap_layer.update_marker(region);
	}

	public void remove_region(final Region region) {
		// Remove region from storage
		if (storage_regions.remove(region.id()) == null) {
			// Was already removed
			return;
		}

		mark_persistent_storage_dirty();

		// Close and taint all related open menus
		get_module().core.menu_manager.for_each_open((player, menu) -> {
			if (menu.tag() instanceof RegionMenuTag
					&& Objects.equals(((RegionMenuTag)menu.tag()).region_id(), region.id())) {
				menu.taint();
				menu.close(player);
			}
		});

		// Remove region from index
		index_remove_region(region);
	}

	private void index_add_region(final Region region) {
		// Adds the region to the lookup map at all intersecting chunks
		final var min = region.extent().min();
		final var max = region.extent().max();

		final var world_id = min.getWorld().getUID();
		var regions_in_chunk = regions_in_chunk_in_world.get(world_id);
		if (regions_in_chunk == null) {
			regions_in_chunk = new HashMap<Long, List<Region>>();
			regions_in_chunk_in_world.put(world_id, regions_in_chunk);
		}

		final var min_chunk = min.getChunk();
		final var max_chunk = max.getChunk();

		// Iterate all the chunks which intersect the region
		for (int cx = min_chunk.getX(); cx <= max_chunk.getX(); ++cx) {
			for (int cz = min_chunk.getZ(); cz <= max_chunk.getZ(); ++cz) {
				final var chunk_key = Chunk.getChunkKey(cx, cz);
				var possible_regions = regions_in_chunk.get(chunk_key);
				if (possible_regions == null) {
					possible_regions = new ArrayList<Region>();
					regions_in_chunk.put(chunk_key, possible_regions);
				}
				possible_regions.add(region);
			}
		}
	}

	private void index_remove_region(final Region region) {
		// Removes the region from the lookup map at all intersecting chunks
		final var min = region.extent().min();
		final var max = region.extent().max();

		final var world_id = min.getWorld().getUID();
		final var regions_in_chunk = regions_in_chunk_in_world.get(world_id);
		if (regions_in_chunk == null) {
			return;
		}

		final var min_chunk = min.getChunk();
		final var max_chunk = max.getChunk();

		// Iterate all the chunks which intersect the region
		for (int cx = min_chunk.getX(); cx <= max_chunk.getX(); ++cx) {
			for (int cz = min_chunk.getZ(); cz <= max_chunk.getZ(); ++cz) {
				final var chunk_key = Chunk.getChunkKey(cx, cz);
				final var possible_regions = regions_in_chunk.get(chunk_key);
				if (possible_regions == null) {
					continue;
				}
				possible_regions.remove(region);
			}
		}
	}

	public Region region_at(final Location loc) {
		final var world_id = loc.getWorld().getUID();
		final var regions_in_chunk = regions_in_chunk_in_world.get(world_id);
		if (regions_in_chunk == null) {
			return null;
		}

		final var chunk_key = loc.getChunk().getChunkKey();
		final var possible_regions = regions_in_chunk.get(chunk_key);
		if (possible_regions == null) {
			return null;
		}

		for (final var region : possible_regions) {
			if (region.extent().is_inside(loc)) {
				return region;
			}
		}

		return null;
	}

	public Region region_at(final Block block) {
		final var world_id = block.getWorld().getUID();
		final var regions_in_chunk = regions_in_chunk_in_world.get(world_id);
		if (regions_in_chunk == null) {
			return null;
		}

		final var chunk_key = block.getChunk().getChunkKey();
		final var possible_regions = regions_in_chunk.get(chunk_key);
		if (possible_regions == null) {
			return null;
		}

		for (final var region : possible_regions) {
			if (region.extent().is_inside(block)) {
				return region;
			}
		}

		return null;
	}

	public RegionGroup get_or_create_default_region_group(final UUID owner) {
		final var region_group_id = storage_default_region_group.get(owner);
		if (region_group_id != null) {
			return get_region_group(region_group_id);
		}

		// Create and save owners's default group
		final var region_group = new RegionGroup("[default]", owner);
		add_region_group(region_group);

		// Set group as the default
		storage_default_region_group.put(owner, region_group.id());
		mark_persistent_storage_dirty();

		return region_group;
	}
}
