package org.oddlama.vane.portals;

import static org.oddlama.vane.util.BlockUtil.adjacent_blocks_3d;
import static org.oddlama.vane.util.ItemUtil.name_item;
import static org.oddlama.vane.util.Nms.item_handle;
import static org.oddlama.vane.util.Nms.register_entity;
import static org.oddlama.vane.util.Nms.spawn;
import static org.oddlama.vane.util.Conversions.ms_to_ticks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;
import org.oddlama.vane.annotation.VaneModule;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigExtendedMaterial;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.annotation.config.ConfigMaterialMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapEntry;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMap;
import org.oddlama.vane.annotation.config.ConfigMaterialMapMapMapEntry;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.annotation.persistent.Persistent;
import org.oddlama.vane.core.functional.Consumer2;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.material.ExtendedMaterial;
import org.oddlama.vane.core.module.Module;
import org.oddlama.vane.core.persistent.PersistentSerializer;
import org.oddlama.vane.portals.entity.FloatingItem;
import org.oddlama.vane.portals.menu.PortalMenuGroup;
import org.oddlama.vane.portals.menu.PortalMenuTag;
import org.oddlama.vane.portals.portal.Orientation;
import org.oddlama.vane.portals.portal.Portal;
import org.oddlama.vane.portals.portal.PortalBlock;
import org.oddlama.vane.portals.portal.PortalBlockLookup;
import org.oddlama.vane.portals.portal.Style;

import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.oddlama.vane.util.StorageUtil;

@VaneModule(name = "portals", bstats = 8642, config_version = 3, lang_version = 5, storage_version = 2)
public class Portals extends Module<Portals> {
	// Add (de-)serializers
	static {
		PersistentSerializer.serializers.put(Orientation.class, x -> ((Orientation) x).name());
		PersistentSerializer.deserializers.put(Orientation.class, x -> Orientation.valueOf((String) x));
		PersistentSerializer.serializers.put(Portal.class, Portal::serialize);
		PersistentSerializer.deserializers.put(Portal.class, Portal::deserialize);
		PersistentSerializer.serializers.put(Portal.Visibility.class, x -> ((Portal.Visibility) x).name());
		PersistentSerializer.deserializers.put(Portal.Visibility.class, x -> Portal.Visibility.valueOf((String) x));
		PersistentSerializer.serializers.put(PortalBlock.class, PortalBlock::serialize);
		PersistentSerializer.deserializers.put(PortalBlock.class, PortalBlock::deserialize);
		PersistentSerializer.serializers.put(PortalBlock.Type.class, x -> ((PortalBlock.Type) x).name());
		PersistentSerializer.deserializers.put(PortalBlock.Type.class, x -> PortalBlock.Type.valueOf((String) x));
		PersistentSerializer.serializers.put(PortalBlockLookup.class, PortalBlockLookup::serialize);
		PersistentSerializer.deserializers.put(PortalBlockLookup.class, PortalBlockLookup::deserialize);
		PersistentSerializer.serializers.put(Style.class, Style::serialize);
		PersistentSerializer.deserializers.put(Style.class, Style::deserialize);
	}

	// TODO better and more default styles
	@ConfigMaterialMapMapMap(
		def = {
			@ConfigMaterialMapMapMapEntry(
				key = "vane_portals:portal_style_default",
				value = {
					@ConfigMaterialMapMapEntry(
						key = "active",
						value = {
							@ConfigMaterialMapEntry(key = "boundary_1", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "boundary_2", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "boundary_3", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "boundary_4", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "boundary_5", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "console", value = Material.ENCHANTING_TABLE),
							@ConfigMaterialMapEntry(key = "origin", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "portal", value = Material.END_GATEWAY),
						}
					),
					@ConfigMaterialMapMapEntry(
						key = "inactive",
						value = {
							@ConfigMaterialMapEntry(key = "boundary_1", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "boundary_2", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "boundary_3", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "boundary_4", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "boundary_5", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "console", value = Material.ENCHANTING_TABLE),
							@ConfigMaterialMapEntry(key = "origin", value = Material.OBSIDIAN),
							@ConfigMaterialMapEntry(key = "portal", value = Material.AIR),
						}
					),
				}
			),
			@ConfigMaterialMapMapMapEntry(
				key = "vane_portals:portal_style_aqua",
				value = {
					@ConfigMaterialMapMapEntry(
						key = "active",
						value = {
							@ConfigMaterialMapEntry(key = "boundary_1", value = Material.DARK_PRISMARINE),
							@ConfigMaterialMapEntry(key = "boundary_2", value = Material.WARPED_PLANKS),
							@ConfigMaterialMapEntry(key = "boundary_3", value = Material.SEA_LANTERN),
							@ConfigMaterialMapEntry(key = "boundary_4", value = Material.WARPED_WART_BLOCK),
							@ConfigMaterialMapEntry(key = "boundary_5", value = Material.LIGHT_BLUE_STAINED_GLASS),
							@ConfigMaterialMapEntry(key = "console", value = Material.ENCHANTING_TABLE),
							@ConfigMaterialMapEntry(key = "origin", value = Material.DARK_PRISMARINE),
							@ConfigMaterialMapEntry(key = "portal", value = Material.END_GATEWAY),
						}
					),
					@ConfigMaterialMapMapEntry(
						key = "inactive",
						value = {
							@ConfigMaterialMapEntry(key = "boundary_1", value = Material.DARK_PRISMARINE),
							@ConfigMaterialMapEntry(key = "boundary_2", value = Material.WARPED_PLANKS),
							@ConfigMaterialMapEntry(key = "boundary_3", value = Material.PRISMARINE_BRICKS),
							@ConfigMaterialMapEntry(key = "boundary_4", value = Material.WARPED_WART_BLOCK),
							@ConfigMaterialMapEntry(key = "boundary_5", value = Material.LIGHT_BLUE_STAINED_GLASS),
							@ConfigMaterialMapEntry(key = "console", value = Material.ENCHANTING_TABLE),
							@ConfigMaterialMapEntry(key = "origin", value = Material.DARK_PRISMARINE),
							@ConfigMaterialMapEntry(key = "portal", value = Material.AIR),
						}
					),
				}
			),
		},
		desc = "Portal style definitions. Must provide a material for each portal block type and activation state. The default style may be overridden."
	)
	public Map<String, Map<String, Map<String, Material>>> config_styles;

	@ConfigLong(
		def = 10000,
		min = 1000,
		max = 110000,
		desc = "Delay in milliseconds after which two connected portals will automatically be disabled. Purple end-gateway beams do not show up until the maximum value of 110 seconds."
	)
	public long config_deactivation_delay;

	@ConfigExtendedMaterial(
		def = "vane:decoration_end_portal_orb",
		desc = "The default portal icon. Also accepts heads from the head library."
	)
	public ExtendedMaterial config_default_icon;

	@ConfigDouble(
		def = 0.9,
		min = 0.0,
		max = 1.0,
		desc = "Volume for the portal activation sound effect. 0 to disable."
	)
	public double config_volume_activation;

	@ConfigDouble(
		def = 1.0,
		min = 0.0,
		max = 1.0,
		desc = "Volume for the portal deactivation sound effect. 0 to disable."
	)
	public double config_volume_deactivation;

	@LangMessage
	public TranslatedMessage lang_console_display_active;

	@LangMessage
	public TranslatedMessage lang_console_display_inactive;

	@LangMessage
	public TranslatedMessage lang_console_no_target;

	@LangMessage
	public TranslatedMessage lang_unlink_restricted;

	@LangMessage
	public TranslatedMessage lang_destroy_restricted;

	@LangMessage
	public TranslatedMessage lang_settings_restricted;

	@LangMessage
	public TranslatedMessage lang_select_target_restricted;

	// This permission allows players (usually admins) to always modify settings
	// on any portal, regardless of whether other restrictions would block access.
	public final Permission admin_permission;

	// Primary storage for all portals (portal_id → portal)
	@Persistent
	private Map<UUID, Portal> storage_portals = new HashMap<>();
	private Map<UUID, Portal> portals = new HashMap<>();

	// Index for all portal blocks (world_id → chunk key → block key → portal block)
	private Map<UUID, Map<Long, Map<Long, PortalBlockLookup>>> portal_blocks_in_chunk_in_world = new HashMap<>();

	// All loaded styles
	public Map<NamespacedKey, Style> styles = new HashMap<>();
	// Cache possible area materials. This is fine as only predefined styles can change this.
	public Set<Material> portal_area_materials = new HashSet<>();

	// Track console items
	private final Map<Block, FloatingItem> console_floating_items = new HashMap<>();
	// Connected portals (always stores both directions!)
	private final Map<UUID, UUID> connected_portals = new HashMap<>();
	// Unloading ticket counter per chunk
	private final Map<Long, Integer> chunk_ticket_count = new HashMap<>();
	// Disable tasks for portals
	private final Map<UUID, BukkitTask> disable_tasks = new HashMap<>();

	public PortalMenuGroup menus;
	public PortalConstructor constructor;
	public PortalDynmapLayer dynmap_layer;
	public PortalBlueMapLayer blue_map_layer;

	public Portals() {
		register_entities();

		menus = new PortalMenuGroup(this);
		new PortalActivator(this);
		new PortalBlockProtector(this);
		constructor = new PortalConstructor(this);
		new PortalTeleporter(this);
		new EntityMoveProcessor(this);
		dynmap_layer = new PortalDynmapLayer(this);
		blue_map_layer = new PortalBlueMapLayer(this);

		// Register admin permission
		admin_permission =
			new Permission(
				"vane." + get_module().get_name() + ".admin",
				"Allows administration of any portal",
				PermissionDefault.OP
			);
		get_module().register_permission(admin_permission);

		// TODO legacy, remove in v2.
		persistent_storage_manager.add_migration_to(
			2,
			"Portal visibility GROUP_INTERNAL was added. This is a no-op.",
			json -> {}
		);
	}

	@SuppressWarnings("unchecked")
	private void register_entities() {
		get_module().core.unfreeze_registries();
		register_entity(
			NamespacedKey.minecraft("item"),
			namespace(),
			"floating_item",
			EntityType.Builder.of(FloatingItem::new, MobCategory.MISC).sized(0.0f, 0.0f)
		);
	}

	private static long block_key(final Block block) {
		return (block.getY() << 8)
			| ((block.getX() & 0xF) << 4)
			| ((block.getZ() & 0xF));
	}

	private static Block unpack_block_key(final Chunk chunk, long block_key) {
		int y = (int)(block_key >> 8);
		int x = (int)((block_key >> 4) & 0xF);
		int z = (int)(block_key & 0xF);
		return chunk.getBlock(x, y, z);
	}

	@Override
	public void on_config_change() {
		styles.clear();

		config_styles.forEach((style_key, v1) -> {
			final var split = style_key.split(":");
			if (split.length != 2) {
				throw new RuntimeException("Invalid style key: '" + style_key + "' is not a valid namespaced key");
			}

			final var style = new Style(StorageUtil.namespaced_key(split[0], split[1]));
			v1.forEach((is_active, v2) -> {
				final boolean active;
				switch (is_active) {
					case "active":
						active = true;
						break;
					case "inactive":
						active = false;
						break;
					default:
						throw new RuntimeException("Invalid active state, must be either 'active' or 'inactive'");
				}

				v2.forEach((portal_block_type, material) -> {
					final var type = PortalBlock.Type.valueOf(portal_block_type.toUpperCase());
					style.set_material(active, type, material);
				});
			});

			// Check validity and add to map.
			style.check_valid();
			styles.put(style.key(), style);
		});

		if (!styles.containsKey(Style.default_style_key())) {
			// Add default style if it wasn't overridden
			final var default_style = Style.default_style();
			styles.put(default_style.key(), default_style);
		}

		portal_area_materials.clear();
		// Acquire material set from styles. Will be used to accelerate event checking.
		for (final var style : styles.values()) {
			portal_area_materials.add(style.material(true, PortalBlock.Type.PORTAL));
		}
	}

	// Lightweight callbacks to the regions module, if it is installed.
	// Lifting the callback storage into the portals module saves us
	// from having to ship regions api with this module.
	private Function2<Portal, Portal, Boolean> is_in_same_region_group_callback = null;

	public void set_is_in_same_region_group_callback(final Function2<Portal, Portal, Boolean> callback) {
		is_in_same_region_group_callback = callback;
	}

	private Function2<Player, Portal, Boolean> player_can_use_portals_in_region_group_of_callback = null;

	public void set_player_can_use_portals_in_region_group_of_callback(
		final Function2<Player, Portal, Boolean> callback
	) {
		player_can_use_portals_in_region_group_of_callback = callback;
	}

	public boolean is_in_same_region_group(final Portal a, final Portal b) {
		if (is_in_same_region_group_callback == null) {
			return true;
		}
		return is_in_same_region_group_callback.apply(a, b);
	}

	public boolean player_can_use_portals_in_region_group_of(final Player player, final Portal portal) {
		if (player_can_use_portals_in_region_group_of_callback == null) {
			return true;
		}
		return player_can_use_portals_in_region_group_of_callback.apply(player, portal);
	}

	public boolean is_regions_installed() {
		return is_in_same_region_group_callback != null;
	}

	public Style style(final NamespacedKey key) {
		final var s = styles.get(key);
		if (s == null) {
			log.warning("Encountered invalid style " + key + ", falling back to default style.");
			return styles.get(Style.default_style_key());
		} else {
			return s;
		}
	}

	public void remove_portal(final Portal portal) {
		// Deactivate portal if needed
		final var connected = connected_portal(portal);
		if (connected != null) {
			disconnect_portals(portal, connected);
		}

		// Remove portal from storage
		if (portals.remove(portal.id()) == null) {
			// Was already removed
			return;
		}

		// Remove portal blocks
		portal.blocks().forEach(this::remove_portal_block);

		// Replace references to the portal everywhere
		// and update all changed portal consoles.
		for (final var other : portals.values()) {
			if (Objects.equals(other.target_id(), portal.id())) {
				other.target_id(null);
				other
					.blocks()
					.stream()
					.filter(pb -> pb.type() == PortalBlock.Type.CONSOLE)
					.filter(pb -> console_floating_items.containsKey(pb.block()))
					.forEach(pb -> update_console_item(other, pb.block()));
			}
		}

		// Force update storage now, as a precaution.
		update_persistent_data();

		// Close and taint all related open menus
		get_module().core.menu_manager.for_each_open((player, menu) -> {
				if (
					menu.tag() instanceof PortalMenuTag &&
					Objects.equals(((PortalMenuTag) menu.tag()).portal_id(), portal.id())
				) {
					menu.taint();
					menu.close(player);
				}
			});

		// Remove map marker
		remove_marker(portal.id());

		// Play sound
		portal
			.spawn()
			.getWorld()
			.playSound(portal.spawn(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS, 1.0f, 1.0f);
	}

	public void add_new_portal(final Portal portal) {
		portal.invalidated = true;

		// Index the new portal
		index_portal(portal);

		// Play sound
		portal
			.spawn()
			.getWorld()
			.playSound(portal.spawn(), Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.BLOCKS, 1.0f, 2.0f);
	}

	public void index_portal(final Portal portal) {
		portals.put(portal.id(), portal);
		portal.blocks().forEach(b -> index_portal_block(portal, b));

		// Create map marker
		update_marker(portal);
	}

	public Collection<Portal> all_available_portals() {
		return portals.values().stream()
			.filter(p -> p.spawn().isWorldLoaded())
			.collect(Collectors.toList());
	}

	public void remove_portal_block(final PortalBlock portal_block) {
		// Restore original block
		switch (portal_block.type()) {
			case ORIGIN:     portal_block.block().setType(constructor.config_material_origin); break;
			case CONSOLE:    portal_block.block().setType(constructor.config_material_console); break;
			case BOUNDARY_1: portal_block.block().setType(constructor.config_material_boundary_1); break;
			case BOUNDARY_2: portal_block.block().setType(constructor.config_material_boundary_2); break;
			case BOUNDARY_3: portal_block.block().setType(constructor.config_material_boundary_3); break;
			case BOUNDARY_4: portal_block.block().setType(constructor.config_material_boundary_4); break;
			case BOUNDARY_5: portal_block.block().setType(constructor.config_material_boundary_5); break;
			case PORTAL:     portal_block.block().setType(constructor.config_material_portal_area); break;
		}

		// Remove console item if block is a console
		if (portal_block.type() == PortalBlock.Type.CONSOLE) {
			remove_console_item(portal_block.block());
		}

		// Remove from acceleration structure
		final var block = portal_block.block();
		final var portal_blocks_in_chunk = portal_blocks_in_chunk_in_world.get(block.getWorld().getUID());
		if (portal_blocks_in_chunk == null) {
			return;
		}

		final var chunk_key = block.getChunk().getChunkKey();
		final var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			return;
		}

		block_to_portal_block.remove(block_key(block));

		// Spawn effect if not portal area
		if (portal_block.type() != PortalBlock.Type.PORTAL) {
			portal_block.block().getWorld()
				.spawnParticle(Particle.ENCHANTMENT_TABLE, portal_block.block().getLocation().add(0.5, 0.5, 0.5), 50, 0.0, 0.0, 0.0, 1.0);
		}
	}

	public void remove_portal_block(final Portal portal, final PortalBlock portal_block) {
		// Remove from portal
		portal.blocks().remove(portal_block);

		// Remove from acceleration structure
		remove_portal_block(portal_block);
	}

	public void add_new_portal_block(final Portal portal, final PortalBlock portal_block) {
		// Add to portal
		portal.blocks().add(portal_block);
		portal.invalidated = true;

		index_portal_block(portal, portal_block);

		// Spawn effect if not portal area
		if (portal_block.type() != PortalBlock.Type.PORTAL) {
			portal_block.block().getWorld()
				.spawnParticle(Particle.PORTAL, portal_block.block().getLocation().add(0.5, 0.5, 0.5), 50, 0.0, 0.0, 0.0, 1.0);
		}
	}

	public void index_portal_block(final Portal portal, final PortalBlock portal_block) {
		// Add to acceleration structure
		final var block = portal_block.block();
		final var world_id = block.getWorld().getUID();
		var portal_blocks_in_chunk = portal_blocks_in_chunk_in_world.computeIfAbsent(world_id, k -> new HashMap<>());

		final var chunk_key = block.getChunk().getChunkKey();
		var block_to_portal_block = portal_blocks_in_chunk.computeIfAbsent(chunk_key, k -> new HashMap<>());

		block_to_portal_block.put(block_key(block), portal_block.lookup(portal.id()));
	}

	public PortalBlockLookup portal_block_for(final Block block) {
		final var portal_blocks_in_chunk = portal_blocks_in_chunk_in_world.get(block.getWorld().getUID());
		if (portal_blocks_in_chunk == null) {
			return null;
		}

		final var chunk_key = block.getChunk().getChunkKey();
		final var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			return null;
		}

		return block_to_portal_block.get(block_key(block));
	}

	public Portal portal_for(@Nullable final UUID uuid) {
		final var portal = portals.get(uuid);
		if (portal == null || !portal.spawn().isWorldLoaded()) {
			return null;
		}
		return portal;
	}

	public Portal portal_for(@NotNull final PortalBlockLookup block) {
		return portal_for(block.portal_id());
	}

	public Portal portal_for(final Block block) {
		final var portal_block = portal_block_for(block);
		if (portal_block == null) {
			return null;
		}

		return portal_for(portal_block);
	}

	public boolean is_portal_block(final Block block) {
		final var portal_blocks_in_chunk = portal_blocks_in_chunk_in_world.get(block.getWorld().getUID());
		if (portal_blocks_in_chunk == null) {
			return false;
		}

		final var chunk_key = block.getChunk().getChunkKey();
		final var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			return false;
		}

		return block_to_portal_block.containsKey(block_key(block));
	}

	public Portal controlled_portal(final Block block) {
		final var root_portal = portal_for(block);
		if (root_portal != null) {
			return root_portal;
		}

		// Find adjacent console blocks in full 3x3x3 cube, which will make this block a controlling block
		for (final var adj : adjacent_blocks_3d(block)) {
			final var portal_block = portal_block_for(adj);
			if (portal_block != null && portal_block.type() == PortalBlock.Type.CONSOLE) {
				return portal_for(portal_block);
			}
		}

		return null;
	}

	public Set<Chunk> chunks_for(final Portal portal) {
		if (portal == null) {
			return new HashSet<Chunk>();
		}

		final var set = new HashSet<Chunk>();
		for (final var pb : portal.blocks()) {
			set.add(pb.block().getChunk());
		}
		return set;
	}

	public void load_portal_chunks(final Portal portal) {
		// Load chunks and adds a ticket so they get loaded and are kept loaded
		for (final var chunk : chunks_for(portal)) {
			final var chunk_key = chunk.getChunkKey();
			final var ticket_counter = chunk_ticket_count.get(chunk_key);
			if (ticket_counter == null) {
				chunk.addPluginChunkTicket(this);
				chunk_ticket_count.put(chunk_key, 1);
			} else {
				chunk_ticket_count.put(chunk_key, ticket_counter + 1);
			}
		}
	}

	public void allow_unload_portal_chunks(final Portal portal) {
		// Removes the ticket so chunks can be unloaded again
		for (final var chunk : chunks_for(portal)) {
			final var chunk_key = chunk.getChunkKey();
			final var ticket_counter = chunk_ticket_count.get(chunk_key);

			if (ticket_counter > 1) {
				chunk_ticket_count.put(chunk_key, ticket_counter - 1);
			} else if (ticket_counter == 1) {
				chunk.removePluginChunkTicket(this);
				chunk_ticket_count.remove(chunk_key);
			}
		}
	}

	public void connect_portals(final Portal src, final Portal dst) {
		// Load chunks
		load_portal_chunks(src);
		load_portal_chunks(dst);

		// Add to map
		connected_portals.put(src.id(), dst.id());
		connected_portals.put(dst.id(), src.id());

		// Activate both
		src.on_connect(this, dst);
		dst.on_connect(this, src);

		// Schedule automatic disable
		start_disable_task(src, dst);
	}

	public void disconnect_portals(final Portal src) {
		disconnect_portals(src, portal_for(connected_portals.get(src.id())));
	}

	public void disconnect_portals(final Portal src, final Portal dst) {
		if (src == null || dst == null) {
			return;
		}

		// Allow unloading chunks again
		allow_unload_portal_chunks(src);
		allow_unload_portal_chunks(dst);

		// Remove from map
		connected_portals.remove(src.id());
		connected_portals.remove(dst.id());

		// Deactivate both
		src.on_disconnect(this, dst);
		dst.on_disconnect(this, src);

		// Reset target id's if the target portal was transient and
		// the target isn't locked.
		if (dst.visibility().is_transient_target() && !src.target_locked()) {
			src.target_id(null);
			src.update_blocks(this);
		}

		// Remove automatic disable task if existing
		stop_disable_task(src, dst);
	}

	private void start_disable_task(final Portal portal, final Portal target) {
		stop_disable_task(portal, target);
		final var task = schedule_task(
			new PortalDisableRunnable(portal, target),
			ms_to_ticks(config_deactivation_delay)
		);
		disable_tasks.put(portal.id(), task);
		disable_tasks.put(target.id(), task);
	}

	private void stop_disable_task(final Portal portal, final Portal target) {
		final var task1 = disable_tasks.remove(portal.id());
		final var task2 = disable_tasks.remove(target.id());
		if (task1 != null) {
			task1.cancel();
		}
		if (task2 != null && task2 != task1) {
			task2.cancel();
		}
	}

	@Override
	public void on_disable() {
		// Disable all portals now
		for (final var id : new ArrayList<>(connected_portals.keySet())) {
			disconnect_portals(portal_for(id));
		}

		// Remove all console items, and all chunk tickets
		chunk_ticket_count.clear();
		for (final var world : getServer().getWorlds()) {
			for (final var chunk : world.getLoadedChunks()) {
				// Remove console item
				for_each_console_block_in_chunk(chunk, (block, console) -> remove_console_item(block));
				// Allow chunk unloading
				chunk.removePluginChunkTicket(this);
			}
		}

		// Save data
		update_persistent_data();
		super.on_disable();
	}

	public boolean is_activated(final Portal portal) {
		return connected_portals.containsKey(portal.id());
	}

	public Portal connected_portal(final Portal portal) {
		final var connected_id = connected_portals.get(portal.id());
		if (connected_id == null) {
			return null;
		}
		return portal_for(connected_id);
	}

	public ItemStack icon_for(final Portal portal) {
		final var item = portal.icon();
		if (item == null) {
			return config_default_icon.item();
		} else {
			return item;
		}
	}

	private ItemStack make_console_item(final Portal portal, boolean active) {
		final Portal target;
		if (active) {
			target = connected_portal(portal);
		} else {
			target = portal.target(this);
		}

		// Try to use target portal's block
		ItemStack item = null;
		if (target != null) {
			item = target.icon();
		}

		// Fallback item
		if (item == null) {
			item = config_default_icon.item();
		}

		final var target_name = target == null ? lang_console_no_target.str() : target.name();
		final Component display_name;
		if (active) {
			display_name = lang_console_display_active.format("§5" + target_name);
		} else {
			display_name = lang_console_display_inactive.format("§7" + target_name);
		}

		return name_item(item, display_name);
	}

	public void update_portal_icon(final Portal portal) {
		// Update map marker, as name could have changed
		update_marker(portal);

		for (final var active_console : console_floating_items.keySet()) {
			final var portal_block = portal_block_for(active_console);
			final var other = portal_for(portal_block);
			if (Objects.equals(other.target_id(), portal.id())) {
				update_console_item(other, active_console);
			}
		}
	}

	public void update_portal_visibility(final Portal portal) {
		// Replace references to the portal everywhere, if visibility
		// has changed.
		switch (portal.visibility()) {
			case PRIVATE:
			case GROUP:
				// Not visible from outside, these are transient.
				for (final var other : portals.values()) {
					if (Objects.equals(other.target_id(), portal.id())) {
						other.target_id(null);
					}
				}
				break;
			case GROUP_INTERNAL:
				// Remove from portals outside of the group
				for (final var other : portals.values()) {
					if (Objects.equals(other.target_id(), portal.id()) && !is_in_same_region_group(other, portal)) {
						other.target_id(null);
					}
				}
				break;
			default: // Nothing to do
				break;
		}

		// Update map marker
		update_marker(portal);
	}

	public void update_console_item(final Portal portal, final Block block) {
		var console_item = console_floating_items.get(block);
		final boolean is_new;
		if (console_item == null) {
			console_item =
				new FloatingItem(block.getWorld(), block.getX() + 0.5, block.getY() + 1.2, block.getZ() + 0.5);
			is_new = true;
		} else {
			is_new = false;
		}

		final var active = is_activated(portal);
		console_item.setItem(item_handle(make_console_item(portal, active)));

		if (is_new) {
			console_floating_items.put(block, console_item);
			spawn(block.getWorld(), console_item);
		}
	}

	public void remove_console_item(final Block block) {
		final var console_item = console_floating_items.remove(block);
		if (console_item != null) {
			console_item.discard();
		}
	}

	private void for_each_console_block_in_chunk(
		final Chunk chunk,
		final Consumer2<Block, PortalBlockLookup> consumer
	) {
		final var portal_blocks_in_chunk = portal_blocks_in_chunk_in_world.get(chunk.getWorld().getUID());
		if (portal_blocks_in_chunk == null) {
			return;
		}

		final var chunk_key = chunk.getChunkKey();
		final var block_to_portal_block = portal_blocks_in_chunk.get(chunk_key);
		if (block_to_portal_block == null) {
			return;
		}

		block_to_portal_block.forEach((k, v) -> {
			if (v.type() == PortalBlock.Type.CONSOLE) {
				consumer.apply(unpack_block_key(chunk, k), v);
			}
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_monitor_chunk_unload(final ChunkUnloadEvent event) {
		final var chunk = event.getChunk();

		// Disable all consoles in this chunk
		for_each_console_block_in_chunk(chunk, (block, console) -> remove_console_item(block));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void on_monitor_chunk_load(final ChunkLoadEvent event) {
		final var chunk = event.getChunk();

		// Enable all consoles in this chunk
		for_each_console_block_in_chunk(
			chunk,
			(block, console) -> {
				final var portal = portal_for(console.portal_id());
				update_console_item(portal, block);
			}
		);
	}

	public void update_marker(final Portal portal) {
		dynmap_layer.update_marker(portal);
		blue_map_layer.update_marker(portal);
	}

	public void remove_marker(final UUID portal_id) {
		dynmap_layer.remove_marker(portal_id);
		blue_map_layer.remove_marker(portal_id);
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

	public void update_persistent_data() {
		for (final var world : getServer().getWorlds()) {
			update_persistent_data(world);
		}
	}

	public static final NamespacedKey STORAGE_PORTALS = StorageUtil.namespaced_key("vane_portals", "portals");

	public void load_persistent_data(final World world) {
		final var data = world.getPersistentDataContainer();
		final var storage_portal_prefix = STORAGE_PORTALS + ".";

		// Load all currently stored portals.
		final var pdc_portals = data.getKeys().stream()
			.filter(key -> key.toString().startsWith(storage_portal_prefix))
			.map(key -> StringUtils.removeStart(key.toString(), storage_portal_prefix))
			.map(uuid -> UUID.fromString(uuid))
			.collect(Collectors.toSet());

		for (final var portal_id : pdc_portals) {
			final var json_bytes = data.get(NamespacedKey.fromString(storage_portal_prefix + portal_id.toString()),
				PersistentDataType.BYTE_ARRAY);
			try {
				final var portal = PersistentSerializer.from_json(Portal.class, new JSONObject(new String(json_bytes)));
				index_portal(portal);
			} catch (IOException e) {
				log.log(Level.SEVERE, "error while serializing persistent data!", e);
			}
		}
		log.log(Level.INFO, "Loaded " + pdc_portals.size() + " portals for world " + world.getName() + "(" + world.getUID() + ")");

		// Convert portals from legacy storage
		final Set<UUID> remove_from_legacy_storage = new HashSet<>();
		int converted = 0;
		for (final var portal : storage_portals.values()) {
			if (!portal.spawn_world().equals(world.getUID())) {
				continue;
			}

			if (portals.containsKey(portal.id())) {
				remove_from_legacy_storage.add(portal.id());
				continue;
			}

			index_portal(portal);
			portal.invalidated = true;
			converted += 1;
		}

		// Remove any portal that was successfully loaded from the new storage.
		remove_from_legacy_storage.forEach(storage_portals::remove);
		if (remove_from_legacy_storage.size() > 0) {
			mark_persistent_storage_dirty();
		}

		// Update all consoles in the loaded world. These
		// might be missed by chunk load event as it runs asynchronous
		// to this function, and it can't be synchronized without annoying the server.
		for (final var chunk : world.getLoadedChunks()) {
			for_each_console_block_in_chunk(
				chunk,
				(block, console) -> {
					final var portal = portal_for(console.portal_id());
					update_console_item(portal, block);
				}
			);
		}

		// Save if we had any conversions
		if (converted > 0) {
			update_persistent_data();
		}
	}

	public void update_persistent_data(final World world) {
		final var data = world.getPersistentDataContainer();
		final var storage_portal_prefix = STORAGE_PORTALS + ".";

		// Update invalidated portals
		portals.values().stream()
			.filter(x -> x.invalidated && x.spawn_world().equals(world.getUID()))
			.forEach(portal -> {
				try {
					final var json = PersistentSerializer.to_json(Portal.class, portal);
					data.set(NamespacedKey.fromString(storage_portal_prefix + portal.id().toString()),
						PersistentDataType.BYTE_ARRAY, json.toString().getBytes());
				} catch (IOException e) {
					log.log(Level.SEVERE, "error while serializing persistent data!", e);
					return;
				}

				portal.invalidated = false;
			});

		// Get all currently stored portals.
		final var stored_portals = data.getKeys().stream()
			.filter(key -> key.toString().startsWith(storage_portal_prefix))
			.map(key -> StringUtils.removeStart(key.toString(), storage_portal_prefix))
			.map(uuid -> UUID.fromString(uuid))
			.collect(Collectors.toSet());

		// Remove all portals that no longer exist
		Sets.difference(stored_portals, portals.keySet()).forEach(id -> data.remove(NamespacedKey.fromString(storage_portal_prefix + id.toString())));
	}

	private class PortalDisableRunnable implements Runnable {

		private Portal src;
		private Portal dst;

		public PortalDisableRunnable(final Portal src, final Portal dst) {
			this.src = src;
			this.dst = dst;
		}

		@Override
		public void run() {
			Portals.this.disconnect_portals(src, dst);
		}
	}
}
