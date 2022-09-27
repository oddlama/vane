package org.oddlama.vane.regions;

import static org.oddlama.vane.util.PlayerUtil.take_items;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.json.JSONObject;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigBoolean;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigInt;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.portals.Portals;
import org.oddlama.vane.regions.menu.RegionGroupMenuTag;
import org.oddlama.vane.regions.menu.RegionMenuGroup;
import org.oddlama.vane.regions.menu.RegionMenuTag;
import org.oddlama.vane.regions.region.EnvironmentSetting;
import org.oddlama.vane.regions.region.Region;
import org.oddlama.vane.regions.region.RegionExtent;
import org.oddlama.vane.regions.region.RegionGroup;
import org.oddlama.vane.regions.region.RegionSelection;
import org.oddlama.vane.regions.region.Role;
import org.oddlama.vane.regions.region.RoleSetting;
import org.oddlama.vane.util.StorageUtil;

import net.minecraft.core.BlockPos;

@VaneModule(name = "regions", bstats = 8643, config_version = 4, lang_version = 3, storage_version = 1)
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

	// Add (de-)serializers
	static {
		PersistentSerializer.serializers.put(EnvironmentSetting.class, x -> ((EnvironmentSetting) x).name());
		PersistentSerializer.deserializers.put(EnvironmentSetting.class, x -> EnvironmentSetting.valueOf((String) x));
		PersistentSerializer.serializers.put(RoleSetting.class, x -> ((RoleSetting) x).name());
		PersistentSerializer.deserializers.put(RoleSetting.class, x -> RoleSetting.valueOf((String) x));
		PersistentSerializer.serializers.put(Role.class, Role::serialize);
		PersistentSerializer.deserializers.put(Role.class, Role::deserialize);
		PersistentSerializer.serializers.put(Role.RoleType.class, x -> ((Role.RoleType) x).name());
		PersistentSerializer.deserializers.put(Role.RoleType.class, x -> Role.RoleType.valueOf((String) x));
		PersistentSerializer.serializers.put(RegionGroup.class, RegionGroup::serialize);
		PersistentSerializer.deserializers.put(RegionGroup.class, RegionGroup::deserialize);
		PersistentSerializer.serializers.put(Region.class, Region::serialize);
		PersistentSerializer.deserializers.put(Region.class, Region::deserialize);
		PersistentSerializer.serializers.put(RegionExtent.class, RegionExtent::serialize);
		PersistentSerializer.deserializers.put(RegionExtent.class, RegionExtent::deserialize);
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

	@ConfigBoolean(def = false, desc = "Use economy via VaultAPI as currency provider.")
	public boolean config_economy_as_currency;

	@ConfigInt(
		def = 0,
		min = -1,
		desc = "The amount of decimal places the costs will be rounded to. If set to -1, it will round to the amount of decimal places specified by your economy plugin. If set to 0, costs will simply be rounded up to the nearest integer."
	)
	public int config_economy_decimal_places;

	@ConfigMaterial(
		def = Material.DIAMOND,
		desc = "The currency material for regions. The alternative option to an economy plugin."
	)
	public Material config_currency;

	@ConfigDouble(
		def = 2.0,
		min = 0.0,
		desc = "The base amount of currency required to buy an area equal to one chunk (256 blocks)."
	)
	public double config_cost_xz_base;

	@ConfigDouble(
		def = 1.15,
		min = 1.0,
		desc = "The multiplicator determines how much the cost increases for each additional 16 blocks of height. A region of height h will cost multiplicator^(h / 16.0) * base_amount. Rounding is applied at the end."
	)
	public double config_cost_y_multiplicator;

	// Primary storage for all regions (region.id → region)
	@Persistent
	private Map<UUID, Region> storage_regions = new HashMap<>();
	private Map<UUID, Region> regions = new HashMap<>();

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
	private Map<UUID, RegionSelection> region_selections = new HashMap<>();

	@LangMessage
	public TranslatedMessage lang_start_region_selection;

	// This permission allows players (usually admins) to always administrate
	// any region (rename, delete), regardless of whether other restrictions
	// would block access.
	public final Permission admin_permission;

	public RegionMenuGroup menus;

	public RegionDynmapLayer dynmap_layer;
	public RegionBlueMapLayer blue_map_layer;
	public RegionPlexMapLayer plexmap_layer;

	public RegionEconomyDelegate economy;

	public Regions() {
		menus = new RegionMenuGroup(this);

		new org.oddlama.vane.regions.commands.Region(this);

		new RegionEnvironmentSettingEnforcer(this);
		new RegionRoleSettingEnforcer(this);
		new RegionSelectionListener(this);
		dynmap_layer = new RegionDynmapLayer(this);
		blue_map_layer = new RegionBlueMapLayer(this);
		plexmap_layer = new RegionPlexMapLayer(this);

		// Register admin permission
		admin_permission =
			new Permission(
				"vane." + get_module().get_name() + ".admin",
				"Allows administration of any region",
				PermissionDefault.OP
			);
		get_module().register_permission(admin_permission);
	}

	public void delayed_on_enable() {
		if (config_economy_as_currency) {
			if (!setup_economy()) {
				config_economy_as_currency = false;
			}
		}
	}

	private boolean setup_economy() {
		get_module().log.info("Enabling economy integration");

		Plugin vault_api_plugin;
		try {
			vault_api_plugin = get_module().getServer().getPluginManager().getPlugin("Vault");
		} catch (Exception e) {
			get_module()
				.log.severe(
					"Economy was selected as the currency provider, but the Vault plugin wasn't found! Falling back to material currency."
				);
			return false;
		}

		economy = new RegionEconomyDelegate(this);
		return economy.setup(vault_api_plugin);
	}

	@Override
	public void on_enable() {
		final var portals = (Portals) getServer().getPluginManager().getPlugin("vane-portals");

		// Register callback to portals module so portals
		// can find out if two portals are in the same region group
		portals.set_is_in_same_region_group_callback((a, b) -> {
			final var reg_a = region_at(a.spawn());
			final var reg_b = region_at(b.spawn());
			if (reg_a == null || reg_b == null) {
				return reg_a == reg_b;
			}
			return reg_a.region_group_id().equals(reg_b.region_group_id());
		});

		portals.set_player_can_use_portals_in_region_group_of_callback((player, portal) -> {
			final var region = region_at(portal.spawn());
			if (region == null) {
				// No region -> no restriction.
				return true;
			}
			final var group = region.region_group(get_module());
			return group.get_role(player.getUniqueId()).get_setting(RoleSetting.PORTAL);
		});

		schedule_next_tick(this::delayed_on_enable);
		// Every second: Visualize selections
		schedule_task_timer(this::visualize_selections, 1l, 20l);
	}

	public Collection<Region> all_regions() {
		return regions.values().stream()
			.filter(p -> getServer().getWorld(p.extent().world()) != null)
			.collect(Collectors.toList());
	}

	public Collection<RegionGroup> all_region_groups() {
		return storage_region_groups.values();
	}

	public void start_region_selection(final Player player) {
		region_selections.put(player.getUniqueId(), new RegionSelection(this));
		lang_start_region_selection.send(player);
	}

	public void cancel_region_selection(final Player player) {
		region_selections.remove(player.getUniqueId());
	}

	public boolean is_selecting_region(final Player player) {
		return region_selections.containsKey(player.getUniqueId());
	}

	public RegionSelection get_region_selection(final Player player) {
		return region_selections.get(player.getUniqueId());
	}

	private static final int visualize_max_particels = 20000;
	private static final int visualize_particles_per_block = 12;
	private static final double visualize_stddev_compensation = 0.25;
	private static final DustOptions visualize_dust_invalid = new DustOptions(Color.fromRGB(230, 60, 11), 1.0f);
	private static final DustOptions visualize_dust_valid = new DustOptions(Color.fromRGB(120, 220, 60), 1.0f);

	private void visualize_edge(final World world, final BlockPos c1, final BlockPos c2, final boolean valid) {
		// Unfortunately, particle spawns are normal distributed.
		// To still have a good visualization, we need to calculate a stddev that looks
		// good. Empirically we chose a 1/2 of the radius.
		final double mx = (c1.getX() + c2.getX()) / 2.0 + 0.5;
		final double my = (c1.getY() + c2.getY()) / 2.0 + 0.5;
		final double mz = (c1.getZ() + c2.getZ()) / 2.0 + 0.5;
		double dx = Math.abs(c1.getX() - c2.getX());
		double dy = Math.abs(c1.getY() - c2.getY());
		double dz = Math.abs(c1.getZ() - c2.getZ());
		final double len = dx + dy + dz;
		final int count = Math.min(visualize_max_particels, (int) (visualize_particles_per_block * len));

		// Compensate for using normal distributed particles
		dx *= visualize_stddev_compensation;
		dy *= visualize_stddev_compensation;
		dz *= visualize_stddev_compensation;

		// Spawn base particles
		world.spawnParticle(
			Particle.END_ROD,
			mx,
			my,
			mz,
			count,
			dx,
			dy,
			dz,
			0.0, // speed
			null, // data
			true
		); // force

		// Spawn colored particles indicating validity
		world.spawnParticle(
			Particle.REDSTONE,
			mx,
			my,
			mz,
			count,
			dx,
			dy,
			dz,
			0.0, // speed
			valid ? visualize_dust_valid : visualize_dust_invalid, // data
			true
		); // force
	}

	private void visualize_selections() {
		for (final var selection_owner : region_selections.keySet()) {
			final var selection = region_selections.get(selection_owner);
			if (selection == null) {
				continue;
			}

			// Get player for selection
			final var offline_player = getServer().getOfflinePlayer(selection_owner);
			if (!offline_player.isOnline()) {
				continue;
			}
			final var player = offline_player.getPlayer();

			// Both blocks set
			if (selection.primary == null || selection.secondary == null) {
				continue;
			}

			// Worlds match
			if (!selection.primary.getWorld().equals(selection.secondary.getWorld())) {
				continue;
			}

			// Extent is visualizable. Prepare parameters.
			final var world = selection.primary.getWorld();
			// Check if selection is valid
			final var valid = selection.is_valid(player);

			final var lx = Math.min(selection.primary.getX(), selection.secondary.getX());
			final var ly = Math.min(selection.primary.getY(), selection.secondary.getY());
			final var lz = Math.min(selection.primary.getZ(), selection.secondary.getZ());
			final var hx = Math.max(selection.primary.getX(), selection.secondary.getX());
			final var hy = Math.max(selection.primary.getY(), selection.secondary.getY());
			final var hz = Math.max(selection.primary.getZ(), selection.secondary.getZ());

			// Corners
			final var A = new BlockPos(lx, ly, lz);
			final var B = new BlockPos(hx, ly, lz);
			final var C = new BlockPos(hx, hy, lz);
			final var D = new BlockPos(lx, hy, lz);
			final var E = new BlockPos(lx, ly, hz);
			final var F = new BlockPos(hx, ly, hz);
			final var G = new BlockPos(hx, hy, hz);
			final var H = new BlockPos(lx, hy, hz);

			// Visualize each edge
			visualize_edge(world, A, B, valid);
			visualize_edge(world, B, C, valid);
			visualize_edge(world, C, D, valid);
			visualize_edge(world, D, A, valid);
			visualize_edge(world, E, F, valid);
			visualize_edge(world, F, G, valid);
			visualize_edge(world, G, H, valid);
			visualize_edge(world, H, E, valid);
			visualize_edge(world, A, E, valid);
			visualize_edge(world, B, F, valid);
			visualize_edge(world, C, G, valid);
			visualize_edge(world, D, H, valid);
		}
	}

	public void add_region_group(final RegionGroup group) {
		storage_region_groups.put(group.id(), group);
		mark_persistent_storage_dirty();
	}

	public boolean can_remove_region_group(final RegionGroup group) {
		// Returns true if this region group is unused and can be removed.

		// If this region group is the fallback default group, it is permanent!
		if (storage_default_region_group.containsValue(group.id())) {
			return false;
		}

		// If any region uses this group, we can't remove it.
		return regions.values().stream().noneMatch(r -> r.region_group_id().equals(group.id()));
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
		get_module()
			.core.menu_manager.for_each_open((player, menu) -> {
				if (
					menu.tag() instanceof RegionGroupMenuTag &&
					Objects.equals(((RegionGroupMenuTag) menu.tag()).region_group_id(), group.id())
				) {
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

		// Take currency items / withdraw economy
		final var price = selection.price();
		if (config_economy_as_currency) {
			if (price > 0) {
				final var transaction = economy.withdraw(player, price);
				if (!transaction.transactionSuccess()) {
					log.warning(
						"Player " +
						player +
						" tried to create region '" +
						name +
						"' (cost " +
						price +
						") but the economy plugin failed to withdraw:"
					);
					log.warning("Error message: " + transaction.errorMessage);
					return false;
				}
			}
		} else {
			final var map = new HashMap<ItemStack, Integer>();
			map.put(new ItemStack(config_currency), (int) price);
			if (price > 0 && !take_items(player, map)) {
				return false;
			}
		}

		final var def_region_group = get_or_create_default_region_group(player);
		final var region = new Region(name, player.getUniqueId(), selection.extent(), def_region_group.id());
		add_new_region(region);
		cancel_region_selection(player);
		return true;
	}

	public String currency_string() {
		if (config_economy_as_currency) {
			return economy.currency_name_plural();
		} else {
			return String.valueOf(config_currency).toLowerCase();
		}
	}

	public void add_new_region(final Region region) {
		region.invalidated = true;
		// Index region for fast lookup
		index_region(region);
	}

	public void remove_region(final Region region) {
		// Remove region from storage
		if (regions.remove(region.id()) == null) {
			// Was already removed
			return;
		}

		// Force update storage now, as a precaution.
		update_persistent_data();

		// Close and taint all related open menus
		get_module()
			.core.menu_manager.for_each_open((player, menu) -> {
				if (
					menu.tag() instanceof RegionMenuTag &&
					Objects.equals(((RegionMenuTag) menu.tag()).region_id(), region.id())
				) {
					menu.taint();
					menu.close(player);
				}
			});

		// Remove region from index
		index_remove_region(region);

		// Remove map marker
		remove_marker(region.id());
	}

	public void update_marker(final Region region) {
		dynmap_layer.update_marker(region);
		blue_map_layer.update_marker(region);
		plexmap_layer.update_marker(region);
	}

	public void remove_marker(final UUID region_id) {
		dynmap_layer.remove_marker(region_id);
		blue_map_layer.remove_marker(region_id);
		plexmap_layer.remove_marker(region_id);
	}

	private void index_region(final Region region) {
		regions.put(region.id(), region);

		// Adds the region to the lookup map at all intersecting chunks
		final var min = region.extent().min();
		final var max = region.extent().max();

		final var world_id = min.getWorld().getUID();
		var regions_in_chunk = regions_in_chunk_in_world.computeIfAbsent(world_id, k -> new HashMap<>());

		final var min_chunk = min.getChunk();
		final var max_chunk = max.getChunk();

		// Iterate all the chunks which intersect the region
		for (int cx = min_chunk.getX(); cx <= max_chunk.getX(); ++cx) {
			for (int cz = min_chunk.getZ(); cz <= max_chunk.getZ(); ++cz) {
				final var chunk_key = Chunk.getChunkKey(cx, cz);
				var possible_regions = regions_in_chunk.computeIfAbsent(chunk_key, k -> new ArrayList<>());
				possible_regions.add(region);
			}
		}

		// Create map marker
		update_marker(region);
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

	public boolean may_administrate(final Player player, final RegionGroup group) {
		return (
			player.getUniqueId().equals(group.owner()) ||
			group.get_role(player.getUniqueId()).get_setting(RoleSetting.ADMIN)
		);
	}

	public boolean may_administrate(final Player player, final Region region) {
		return player.getUniqueId().equals(region.owner()) || player.hasPermission(admin_permission);
	}

	public RegionGroup get_or_create_default_region_group(final Player owner) {
		final var owner_id = owner.getUniqueId();
		final var region_group_id = storage_default_region_group.get(owner_id);
		if (region_group_id != null) {
			return get_region_group(region_group_id);
		}

		// Create and save owners's default group
		final var region_group = new RegionGroup("[default] " + owner.getName(), owner_id);
		add_region_group(region_group);

		// Set group as the default
		storage_default_region_group.put(owner_id, region_group.id());
		mark_persistent_storage_dirty();

		return region_group;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_player_quit(final PlayerQuitEvent event) {
		// Remove pending selection
		cancel_region_selection(event.getPlayer());
	}

	@EventHandler
	public void on_save_world(final WorldSaveEvent event) {
		update_persistent_data(event.getWorld());
	}

	@EventHandler
	public void on_load_world(final WorldLoadEvent event) {
		load_persistent_data(event.getWorld());
	}

	@EventHandler
	public void on_unload_world(final WorldUnloadEvent event) {
		// Save data before unloading a world (not called on stop)
		update_persistent_data(event.getWorld());
	}

	public static final NamespacedKey STORAGE_REGIONS = StorageUtil.namespaced_key("vane_regions", "regions");

	public void load_persistent_data(final World world) {
		final var data = world.getPersistentDataContainer();
		final var storage_region_prefix = STORAGE_REGIONS + ".";

		// Load all currently stored regions.
		final var pdc_regions = data.getKeys().stream()
			.filter(key -> key.toString().startsWith(storage_region_prefix))
			.map(key -> StringUtils.removeStart(key.toString(), storage_region_prefix))
			.map(uuid -> UUID.fromString(uuid))
			.collect(Collectors.toSet());

		for (final var region_id : pdc_regions) {
			final var json_bytes = data.get(NamespacedKey.fromString(storage_region_prefix + region_id.toString()),
				PersistentDataType.BYTE_ARRAY);
			try {
				final var region = PersistentSerializer.from_json(Region.class, new JSONObject(new String(json_bytes)));
				index_region(region);
			} catch (IOException e) {
				log.log(Level.SEVERE, "error while serializing persistent data!", e);
			}
		}
		log.log(Level.INFO, "Loaded " + pdc_regions.size() + " regions for world " + world.getName() + "(" + world.getUID() + ")");

		// Convert regions from legacy storage
		final Set<UUID> remove_from_legacy_storage = new HashSet<>();
		int converted = 0;
		for (final var region : storage_regions.values()) {
			if (!region.extent().world().equals(world.getUID())) {
				continue;
			}

			if (regions.containsKey(region.id())) {
				remove_from_legacy_storage.add(region.id());
				continue;
			}

			index_region(region);
			region.invalidated = true;
			converted += 1;
		}

		// Remove any region that was successfully loaded from the new storage.
		remove_from_legacy_storage.forEach(storage_regions::remove);
		if (remove_from_legacy_storage.size() > 0) {
			mark_persistent_storage_dirty();
		}

		// Save if we had any conversions
		if (converted > 0) {
			update_persistent_data();
		}
	}

	public void update_persistent_data() {
		for (final var world : getServer().getWorlds()) {
			update_persistent_data(world);
		}
	}

	public void update_persistent_data(final World world) {
		final var data = world.getPersistentDataContainer();
		final var storage_region_prefix = STORAGE_REGIONS + ".";

		// Update invalidated regions
		regions.values().stream()
			.filter(x -> x.invalidated && x.extent().world().equals(world.getUID()))
			.forEach(region -> {
				try {
					final var json = PersistentSerializer.to_json(Region.class, region);
					data.set(NamespacedKey.fromString(storage_region_prefix + region.id().toString()),
						PersistentDataType.BYTE_ARRAY, json.toString().getBytes());
				} catch (IOException e) {
					log.log(Level.SEVERE, "error while serializing persistent data!", e);
					return;
				}

				region.invalidated = false;
			});

		// Get all currently stored regions.
		final var stored_regions = data.getKeys().stream()
			.filter(key -> key.toString().startsWith(storage_region_prefix))
			.map(key -> StringUtils.removeStart(key.toString(), storage_region_prefix))
			.map(uuid -> UUID.fromString(uuid))
			.collect(Collectors.toSet());

		// Remove all regions that no longer exist
		Sets.difference(stored_regions, regions.keySet()).forEach(id -> data.remove(NamespacedKey.fromString(storage_region_prefix + id.toString())));
	}

	@Override
	public void on_disable() {
		// Save data
		update_persistent_data();
		super.on_disable();
	}
}
